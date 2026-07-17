import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { IonicModule } from '@ionic/angular';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-tabbar',
  templateUrl: './tabbar.component.html',
  styleUrls: ['./tabbar.component.scss'],
  standalone: true,
  imports: [IonicModule, CommonModule]
})
export class TabbarComponent {
  constructor(private router: Router) {}

  isTabActive(tabName: string): boolean {
    const url = this.router.url;
    if (tabName === 'inicio' && (url === '/tabs' || url === '/tabs/')) {
      return true;
    }
    return url.includes(`/tabs/${tabName}`);
  }
}
