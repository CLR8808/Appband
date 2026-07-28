import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { IonicModule } from '@ionic/angular';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-detectado',
  templateUrl: './detectado.component.html',
  styleUrls: ['./detectado.component.scss'],
  standalone: true,
  imports: [IonicModule, CommonModule]
})
export class DetectadoComponent {
  constructor(private router: Router) {}

  goBack() {
    this.router.navigate(['/wifi']);
  }

  conectar() {
    this.router.navigate(['/wifi']);
  }
}

