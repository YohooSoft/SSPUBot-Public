import {ChangeDetectorRef, Component, EventEmitter, inject, Input, OnInit, Output} from '@angular/core'; // 1. 务必引入 inject
import {HttpClient} from '@angular/common/http';
import {CommonModule} from '@angular/common';

// 定义接口
interface Post {
    id?: number;
    postName?: string;
    postReleaseTime?: string;
    postSource?: string;
    postContent?: string;
    postUrl?: string;
    postFiles?: string;
    postContentUsingMarkdown?: string;
    postSimplifiedContent?: string;
}

@Component({
    selector: 'app-detail-modal',
    templateUrl: './detail-modal-component.html',
    standalone: true,
    imports: [
        CommonModule
    ],
    styleUrls: ['./detail-modal-component.scss']
})
export class DetailModalComponent implements OnInit {
    @Input() gridName: string = '';
    @Output() close = new EventEmitter<void>();

    private http = inject(HttpClient);
    private cdr = inject(ChangeDetectorRef);

    data: Post[] = [];
    loading: boolean = true;

    // === 🔴 新增：记录当前选中的文章 ===
    selectedPost: Post | null = null;

    constructor() {
    }

    ngOnInit(): void {
        // ... 原有的加载逻辑保持不变 ...
        if (!this.gridName) {
            this.loading = false;
            return;
        }

        const safeName = encodeURIComponent(this.gridName);
        this.http.get<Post[]>(`/api/posts/list1?source=${safeName}`)
            .subscribe({
                next: (res) => {
                    this.data = res || [];
                    this.loading = false;
                    this.cdr.markForCheck();
                },
                error: (err) => {
                    console.error(err);
                    this.loading = false;
                    this.cdr.markForCheck();
                }
            });
    }

    get parsedFileList(): string[] {
        if (!this.selectedPost?.postFiles) return [];
        try {
            const files = this.selectedPost.postFiles;
            // 如果已经是数组则直接返回，如果是字符串则解析
            const parsed = typeof files === 'string' ? JSON.parse(files) : files;
            // 确保返回的是数组
            return Array.isArray(parsed) ? parsed : [];
        } catch (e) {
            console.warn('文件格式解析失败:', e);
            return []; // 解析失败返回空数组
        }
    }

// 2. 根据后缀名获取图标
    getFileIcon(fileName: string): { icon: string, color: string } {
        if (!fileName) return {icon: '📄', color: '#999'};

        // 获取后缀名 (转小写)
        const ext = fileName.split('.').pop()?.toLowerCase();

        switch (ext) {
            case 'pdf':
                return {icon: '📕', color: '#ff3b30'}; // 红色 PDF
            case 'doc':
            case 'docx':
                return {icon: '📘', color: '#007aff'}; // 蓝色 Word
            case 'xls':
            case 'xlsx':
            case 'csv':
                return {icon: '📊', color: '#34c759'}; // 绿色 Excel
            case 'ppt':
            case 'pptx':
                return {icon: '📙', color: '#ff9500'}; // 橙色 PPT
            case 'jpg':
            case 'jpeg':
            case 'png':
            case 'gif':
            case 'webp':
                return {icon: '🖼️', color: '#5856d6'}; // 紫色 图片
            case 'zip':
            case 'rar':
            case '7z':
                return {icon: '📦', color: '#8e8e93'}; // 灰色 压缩包
            case 'txt':
            case 'md':
                return {icon: '📝', color: '#333'};    // 深色 文本
            case 'mp4':
            case 'mov':
            case 'avi':
                return {icon: '🎬', color: '#ff2d55'}; // 视频
            case 'mp3':
            case 'wav':
                return {icon: '🎵', color: '#af52de'}; // 音频
            default:
                return {icon: '📄', color: '#999'};    // 默认文件
        }
    }

    // === 🔴 新增：查看详情 ===
    viewDetail(event: Event, item: Post) {
        event.preventDefault(); // 阻止链接直接跳转
        this.selectedPost = item; // 设置当前选中项，视图会自动切换
    }

    // === 🔴 新增：返回列表 ===
    backToList() {
        this.selectedPost = null; // 清空选中项，视图切回列表
    }

    closeModal() {
        this.close.emit();
    }

    protected readonly window = window;
}
