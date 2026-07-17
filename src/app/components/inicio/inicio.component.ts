import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { IonicModule } from '@ionic/angular';
import { CommonModule } from '@angular/common';
import { StorageService, Dispositivo } from '../../services/storage';

@Component({
  selector: 'app-inicio',
  templateUrl: './inicio.component.html',
  styleUrls: ['./inicio.component.scss'],
  standalone: true,
  imports: [IonicModule, CommonModule]
})
export class InicioComponent implements OnInit {
  dispositivos: Dispositivo[] = [];

  constructor(private router: Router, private storage: StorageService) {}

  ngOnInit() {
    this.storage.dispositivos$.subscribe(d => {
      this.dispositivos = d;
    });
  }

  agregarDispositivo() {
    this.router.navigate(['/preparar-dispositivo']);
  }

  irADetalle(disp: Dispositivo) {
    this.router.navigate(['/dispositivo']);
  }
}
