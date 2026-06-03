import {Inject, Injectable, PLATFORM_ID} from '@angular/core';
import {IDBPDatabase, openDB} from 'idb';
import {isPlatformBrowser} from '@angular/common';

@Injectable({
    providedIn: 'root',
})
export class LocalIDBService {
    private dbPromise: Promise<IDBPDatabase> | null = null;

    constructor(@Inject(PLATFORM_ID) private platformId: Object) {
        if (isPlatformBrowser(this.platformId)) {
            this.initializeDB();
        }
    }

    private async initializeDB(): Promise<void> {
        if (!isPlatformBrowser(this.platformId)) {
            console.warn('Not running in a browser environment. IndexedDB is not available.');
            return;
        }

        const dbName = 'AppDatabase';
        let currentVersion = 1;

        try {
            const existingDB = await openDB(dbName);
            currentVersion = existingDB.version; // 动态获取当前版本号
            existingDB.close();
        } catch (error) {
            console.warn('Database does not exist or cannot be opened. Using default version 1.');
        }

        this.dbPromise = openDB(dbName, currentVersion, {
            upgrade(db) {
                // 动态创建对象存储时无需预定义
            },
        });
    }

    private async createObjectStore(storeName: string): Promise<void> {
        if (!this.dbPromise) {
            throw new Error('IndexedDB is not available in this environment.');
        }
        const db = await this.dbPromise;
        if (!db.objectStoreNames.contains(storeName)) {
            db.close(); // 关闭旧数据库连接
            const currentVersion = db.version; // 获取当前版本号
            const newVersion = currentVersion + 1; // 确保版本号递增
            this.dbPromise = openDB('AppDatabase', newVersion, {
                upgrade(upgradedDb) {
                    if (!upgradedDb.objectStoreNames.contains(storeName)) {
                        upgradedDb.createObjectStore(storeName); // 创建新的对象存储
                    }
                },
            });
        }
    }

    async getItem<T>(storeName: string, key: string): Promise<T | null> {
        if (!this.dbPromise) {
            console.warn('IndexedDB is not available in this environment.');
            return null;
        }
        try {
            await this.createObjectStore(storeName); // 确保对象存储存在
            const db = await this.dbPromise;
            const transaction = db.transaction(storeName, 'readonly'); // 打开只读事务
            const store = transaction.objectStore(storeName);
            return (await store.get(key)) || null; // 获取数据
        } catch (error) {
            console.error('Error retrieving item from IndexedDB:', error);
            return null;
        }
    }

    async setItem<T>(storeName: string, key: string, value: T): Promise<void> {
        if (!this.dbPromise) {
            console.warn('IndexedDB is not available in this environment. Skipping setItem operation.');
            return;
        }
        try {
            await this.createObjectStore(storeName);
            const db = await this.dbPromise;
            const transaction = db.transaction(storeName, 'readwrite');
            const store = transaction.objectStore(storeName);
            await store.put(value, key);
        } catch (error) {
            console.error('Error setting item in IndexedDB:', error);
        }
    }
}
