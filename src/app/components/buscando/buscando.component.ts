import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { IonicModule } from '@ionic/angular';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-buscando',
  templateUrl: './buscando.component.html',
  styleUrls: ['./buscando.component.scss'],
  standalone: true,
  imports: [IonicModule, CommonModule]
})
export class BuscandoComponent implements OnInit {
  constructor(private router: Router) {}

  ngOnInit() {
    this.router.navigate(['/wifi']);
  }

  cancelar() {
    this.router.navigate(['/preparar-dispositivo']);
  }
}
