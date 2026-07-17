import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { IonicModule } from '@ionic/angular';

@Component({
  selector: 'app-buscando',
  templateUrl: './buscando.component.html',
  styleUrls: ['./buscando.component.scss'],
  standalone: true,
  imports: [IonicModule]
})
export class BuscandoComponent implements OnInit {
  constructor(private router: Router) {}

  ngOnInit() {
    setTimeout(() => {
      this.router.navigate(['/detectado']);
    }, 2000);
  }

  cancelar() {
    this.router.navigate(['/bluetooth']);
  }
}
