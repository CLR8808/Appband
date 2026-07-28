import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { IonicModule } from '@ionic/angular';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { StorageService } from '../../services/storage';

@Component({
  selector: 'app-nombre',
  templateUrl: './nombre.component.html',
  styleUrls: ['./nombre.component.scss'],
  standalone: true,
  imports: [IonicModule, CommonModule, FormsModule]
})
export class NombreComponent {
  nombre = "SafeBand_V1";

  constructor(private router: Router, private storage: StorageService) {}

  goBack() {
    this.router.navigate(['/password']); 
  }

  guardar() {
    this.storage.agregarDispositivo(this.nombre);
    this.router.navigate(['/anadida'], { queryParams: { nombre: this.nombre } });
  }
}
