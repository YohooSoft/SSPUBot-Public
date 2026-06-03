import {ChangeDetectorRef, Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {HttpClient} from '@angular/common/http';
import {DetailModalComponent} from '../detail-modal-component/detail-modal-component';

interface GridData {
    id: number;
    title: string;
    items: itemData[];
}

interface itemData {
    title: string;
    url: string;
}

@Component({
    selector: 'app-home',
    standalone: true,
    imports: [CommonModule, DetailModalComponent],
    templateUrl: './home-component.html',
    styleUrls: ['./home-component.scss']
})
export class HomeComponent implements OnInit {
    // 记录当前被放大的网格 ID，如果没有则为 null
    expandedId: number | null = null;
    gridName: string = '默认网格名称';

    gridList: GridData[] = [];

    constructor(
        public http: HttpClient,
        private cdr: ChangeDetectorRef
    ) {
    }

    ngOnInit(): void {
        this.http.get<GridData[]>("/api/posts/index")
            .subscribe(data => {
                    this.gridList = data;
                    this.cdr.markForCheck();
                }
            );
    }

    // 关闭函数
    closeExpanded() {
        this.expandedId = null;
    }

    getActiveGridItem() {
      // 确保 gridList 存在且 expandedId 有值
      return this.gridList.find(item => item.id === this.expandedId);
    }

    toggleExpand(grid: any): void {
        console.debug('Toggling expand for grid:', grid);
        this.expandedId = grid.id;
        this.gridName = grid.title;
    }
}
