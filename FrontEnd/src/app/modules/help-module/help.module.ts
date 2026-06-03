import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {RouterModule, Routes} from '@angular/router';
import {HelpQueryComponent} from '../../components/help-query-component/help-query-component';
import {HelpStatisticsComponent} from '../../components/help-statistics-component/help-statistics-component';
import {HelpAiComponent} from '../../components/help-ai-component/help-ai-component';
import {HelpAdminComponent} from '../../components/help-admin-component/help-admin-component';
import {HelpProfileComponent} from '../../components/help-profile-component/help-profile-component';
import {HelpSettingsComponent} from '../../components/help-settings-component/help-settings-component';

const routes: Routes = [
    {path: 'query', component: HelpQueryComponent},
    {path: 'statistics', component: HelpStatisticsComponent},
    {path: 'ai', component: HelpAiComponent},
    {path: 'admin', component: HelpAdminComponent},
    {path: 'profile', component: HelpProfileComponent},
    {path: 'settings', component: HelpSettingsComponent},
    {path: '', redirectTo: 'query', pathMatch: 'full'}
];

@NgModule({
    declarations: [],
    imports: [
        CommonModule,
        RouterModule.forChild(routes)
    ]
})
export class HelpModule {
}
