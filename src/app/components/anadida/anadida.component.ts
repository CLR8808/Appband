import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { IonicModule } from '@ionic/angular';

@Component({
  selector: 'app-anadida',
  templateUrl: './anadida.component.html',
  styleUrls: ['./anadida.component.scss'],
  standalone: true,
  imports: [IonicModule]
})
export class AnadidaComponent {
  constructor(private router: Router) {}

  irADispositivos() {
    this.router.navigate(['/tabs/inicio']);
  }
}
