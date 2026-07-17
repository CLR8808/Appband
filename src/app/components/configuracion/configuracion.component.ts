import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { IonicModule } from '@ionic/angular';
import { CommonModule } from '@angular/common';
import { StorageService, Dispositivo } from '../../services/storage';

@Component({
  selector: 'app-configuracion',
  templateUrl: './configuracion.component.html',
  styleUrls: ['./configuracion.component.scss'],
  standalone: true,
  imports: [IonicModule, CommonModule]
})
export class ConfiguracionComponent implements OnInit {
  dispositivos: Dispositivo[] = [];

  constructor(private router: Router, private storage: StorageService) {}

  ngOnInit() {
    this.storage.dispositivos$.subscribe(d => {
      this.dispositivos = d;
    });
  }

  goBack() {
    this.router.navigate(['/tabs/perfil']); 
  }

  desvincular() {
    if (confirm("¿Seguro que quieres desvincular la SafeBand?")) {
      this.storage.eliminarDispositivos();
      this.router.navigate(['/tabs/inicio']);
    }
  }
}
