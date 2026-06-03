import {Routes} from '@angular/router';
import {HomeComponent} from './components/home-component/home-component';
import {AboutComponent} from './components/about-component/about-component';
import {NotFoundComponent} from "./components/not-found-component/not-found-component";
import {SearchComponent} from './components/search-component/search-component';
import {AdvancedSearchComponent} from './components/advanced-search-component/advanced-search-component';
import {SettingsComponent} from './components/settings-component/settings-component';
import {HelpFrameWorkComponent} from './components/help-frame-work-component/help-frame-work-component';
import {LoginComponent} from './components/login-component/login-component';
import {RegisterComponent} from './components/register-component/register-component';
import {AiComponent} from './components/ai-component/ai-component';
import {Statistics} from './components/statistics/statistics';
import {AdminComponent} from './components/admin-component/admin-component';
import {ProfileEditComponent} from './components/profile-edit-component/profile-edit-component';
import {BotMemoryComponent} from './components/bot-memory-component/bot-memory-component';

export const routes: Routes = [
    {path: '', component: HomeComponent},
    {path: 'about', component: AboutComponent},
    {
      path: 'search', component: SearchComponent
    },
    {
        path: 'advancedSearch',
        component: AdvancedSearchComponent
    },

    {
        path: 'settings', component: SettingsComponent
    },
    {
        path: 'profile', component: ProfileEditComponent
    },
    {
        path: 'bot-memory', component: BotMemoryComponent
    },
    {
        path: 'login', component: LoginComponent
    },
    {
        path: 'register',
        component: RegisterComponent
    },
    {
        path: 'help',
        component: HelpFrameWorkComponent,
        loadChildren: () => import('./modules/help-module/help.module').then(m => m.HelpModule)
    },
    {
        path: '404', component: NotFoundComponent
    },
    {
        path: 'chat',
        component: AiComponent
    },
    {
        path: 'statistics',
        component: Statistics
    },
    {
        path: 'admin',
        component: AdminComponent
    },
    {
        path: '**', redirectTo: '/404'
    }
];
