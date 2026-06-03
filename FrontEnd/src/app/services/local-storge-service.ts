import {Injectable} from '@angular/core';

@Injectable({
    providedIn: 'root'
})
export class LocalStorgeService {
    constructor() {
    }

    /**
     * 存储普通字符串
     * @param key
     * @param value
     */
    setItemNormal(key: string, value: any): void {
        if (typeof localStorage === 'undefined') {
            return;
        }
        localStorage.setItem(key, value);
    }

    /**
     * 存储对象
     * @param key
     * @param value
     */
    setItem(key: string, value: any): void {
        if (typeof localStorage === 'undefined') {
            return;
        }
        localStorage.setItem(key, JSON.stringify(value));
    }

    /**
     * 获取普通字符串
     * @param key
     */
    getItemNormal(key: string): string | null {
        if (typeof localStorage === 'undefined') {
            return null;
        }
        return localStorage.getItem(key);
    }

    /**
     * 获取对象
     * @param key
     */
    getItem(key: string): any {
        if (typeof localStorage === 'undefined') {
            return null;
        }
        const item = localStorage.getItem(key);
        return item ? JSON.parse(item) : null;
    }

    /**
     * 删除项目
     * @param key
     */
    removeItem(key: string): void {
        if (typeof localStorage === 'undefined') {
            return;
        }
        localStorage.removeItem(key);
    }

    /**
     * 清空所有存储
     */
    clear(): void {
        if (typeof localStorage === 'undefined') {
            return;
        }
        localStorage.clear();
    }
}
