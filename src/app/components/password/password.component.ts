import { Component, OnInit } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { IonicModule } from '@ionic/angular';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-password',
  templateUrl: './password.component.html',
  styleUrls: ['./password.component.scss'],
  standalone: true,
  imports: [IonicModule, CommonModule, FormsModule]
})
export class PasswordComponent implements OnInit {
  ssid: string = '';
  password = '';
  loading = false;

  constructor(private router: Router, private route: ActivatedRoute) {}

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      this.ssid = params['ssid'] || 'Red Wi-Fi';
    });
  }

  goBack() {
    this.router.navigate(['/wifi']);
  }

  conectarWifi() {
    if (this.password === "123456") {
      this.loading = true;
      setTimeout(() => {
        this.router.navigate(['/nombre']);
      }, 2000);
    } else {
      alert("Contraseña incorrecta");
    }
  }
}
