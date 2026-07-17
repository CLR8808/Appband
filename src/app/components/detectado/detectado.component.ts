import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { IonicModule } from '@ionic/angular';

@Component({
  selector: 'app-detectado',
  templateUrl: './detectado.component.html',
  styleUrls: ['./detectado.component.scss'],
  standalone: true,
  imports: [IonicModule]
})
export class DetectadoComponent {
  constructor(private router: Router) {}

  goBack() {
    this.router.navigate(['/buscando']);
  }

  conectar() {
    this.router.navigate(['/wifi']);
  }
}
