import {ChangeDetectorRef, Component, OnInit} from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgxEchartsModule } from 'ngx-echarts';
import { StatisticsService } from '../../services/statistics.service';
import type { EChartsOption } from 'echarts';

@Component({
  selector: 'app-statistics',
  imports: [CommonModule, NgxEchartsModule],
  templateUrl: './statistics.html',
  styleUrl: './statistics.scss'
})
export class Statistics implements OnInit {
  sourceCountOption: EChartsOption = {};
  dailyCountOption: EChartsOption = {};
  wordCloudOption: EChartsOption = {};
  loading = true;

  // Modal state
  showModal = false;
  modalChartOption: EChartsOption = {};
  modalChartTitle = '';

  constructor(private statisticsService: StatisticsService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.loadStatistics();
  }

  loadStatistics(): void {
    this.loading = true;

    // Load file count by source
    this.statisticsService.getFileCountBySource().subscribe({
      next: (data) => {
        this.sourceCountOption = {
          title: {
            text: '各来源文件数量统计',
            left: 'center'
          },
          tooltip: {
            trigger: 'item',
            formatter: '{b}: {c} ({d}%)'
          },
          legend: {
            orient: 'horizontal',
            bottom: 10,
            left: 'center',
            type: 'scroll',
            pageButtonItemGap: 5,
            pageButtonGap: 20,
            pageIconSize: 12,
            itemGap: 15,
            itemWidth: 25,
            itemHeight: 14,
            textStyle: {
              fontSize: 12
            }
          },
          grid: {
            bottom: 80
          },
          series: [
            {
              name: '文件数量',
              type: 'pie',
              radius: '55%',
              center: ['50%', '45%'],
              data: Object.entries(data).map(([name, value]) => ({
                name,
                value
              })),
              emphasis: {
                itemStyle: {
                  shadowBlur: 10,
                  shadowOffsetX: 0,
                  shadowColor: 'rgba(0, 0, 0, 0.5)'
                }
              }
            }
          ]
        };
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Error loading source count:', err);
        this.loading = false;
        this.cdr.markForCheck();
      }
    });

    // Load daily file count
    this.statisticsService.getDailyFileCount().subscribe({
      next: (data) => {
        const dates = Object.keys(data).sort();
        const values = dates.map(date => data[date]);

        this.dailyCountOption = {
          title: {
            text: '每日发布文件数量',
            left: 'center'
          },
          tooltip: {
            trigger: 'axis',
            axisPointer: {
              type: 'shadow'
            }
          },
          xAxis: {
            type: 'category',
            data: dates,
            axisLabel: {
              rotate: 45,
              interval: Math.floor(dates.length / 10) || 0
            }
          },
          yAxis: {
            type: 'value',
            name: '文件数量'
          },
          series: [
            {
              name: '文件数量',
              type: 'bar',
              data: values,
              itemStyle: {
                color: '#5470c6'
              }
            }
          ],
          dataZoom: [
            {
              type: 'inside',
              start: Math.max(0, 100 - (10 / dates.length) * 100),
              end: 100
            },
            {
              start: Math.max(0, 100 - (10 / dates.length) * 100),
              end: 100
            }
          ]
        };
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Error loading daily count:', err);
        this.cdr.markForCheck();
      }
    });

    // Load word cloud data
    this.statisticsService.getWordCloudData().subscribe({
      next: (data) => {
        const words = Object.keys(data);
        const frequencies = Object.values(data);

        this.wordCloudOption = {
          title: {
            text: '关键词词频统计',
            left: 'center'
          },
          tooltip: {
            trigger: 'axis',
            axisPointer: {
              type: 'shadow'
            }
          },
          xAxis: {
            type: 'category',
            data: words.slice(0, 20), // Show top 20 words
            axisLabel: {
              rotate: 45,
              interval: 0
            }
          },
          yAxis: {
            type: 'value',
            name: '出现次数'
          },
          series: [
            {
              name: '频次',
              type: 'bar',
              data: frequencies.slice(0, 20),
              itemStyle: {
                color: function(params) {
                  const colors = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc'];
                  return colors[params.dataIndex % colors.length];
                }
              }
            }
          ]
        };
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Error loading word cloud:', err);
        this.cdr.markForCheck();
      }
    });
  }

  openModal(chartOption: EChartsOption, title: string): void {
    this.modalChartOption = chartOption;
    this.modalChartTitle = title;
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
  }
}
