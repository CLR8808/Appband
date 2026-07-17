import { Routes } from '@angular/router';

// Auth
import { IndexComponent } from './components/index/index.component';
import { IniciarSesionComponent } from './components/iniciar-sesion/iniciar-sesion.component';
import { CrearCuentaComponent } from './components/crear-cuenta/crear-cuenta.component';
import { VerificarCodigoComponent } from './components/verificar-codigo/verificar-codigo.component';

// Tabs Layout
import { TabbarComponent } from './shared/tabbar/tabbar.component';
import { InicioComponent } from './components/inicio/inicio.component';
import { EventosComponent } from './components/eventos/eventos.component';
import { MapaComponent } from './components/mapa/mapa.component';
import { PerfilComponent } from './components/perfil/perfil.component';

// Aux
import { PrepararDispositivoComponent } from './components/preparar-dispositivo/preparar-dispositivo.component';
import { BluetoothComponent } from './components/bluetooth/bluetooth.component';
import { BuscandoComponent } from './components/buscando/buscando.component';
import { DetectadoComponent } from './components/detectado/detectado.component';
import { WifiComponent } from './components/wifi/wifi.component';
import { PasswordComponent } from './components/password/password.component';
import { NombreComponent } from './components/nombre/nombre.component';
import { AnadidaComponent } from './components/anadida/anadida.component';
import { DispositivoComponent } from './components/dispositivo/dispositivo.component';
import { ConfiguracionComponent } from './components/configuracion/configuracion.component';
import { DetallesUsuarioComponent } from './components/detalles-usuario/detalles-usuario.component';
import { SoporteComponent } from './components/soporte/soporte.component';

export const routes: Routes = [
  {
    path: '',
    component: IndexComponent,
    pathMatch: 'full',
  },
  {
    path: 'iniciar-sesion',
    component: IniciarSesionComponent,
  },
  {
    path: 'crear-cuenta',
    component: CrearCuentaComponent,
  },
  {
    path: 'verificar-codigo',
    component: VerificarCodigoComponent,
  },
  {
    path: 'tabs',
    component: TabbarComponent,
    children: [
      {
        path: 'inicio',
        component: InicioComponent
      },
      {
        path: 'eventos',
        component: EventosComponent
      },
      {
        path: 'mapa',
        component: MapaComponent
      },
      {
        path: 'perfil',
        component: PerfilComponent
      },
      {
        path: '',
        redirectTo: 'inicio',
        pathMatch: 'full'
      }
    ]
  },
  {
    path: 'preparar-dispositivo',
    component: PrepararDispositivoComponent
  },
  {
    path: 'bluetooth',
    component: BluetoothComponent
  },
  {
    path: 'buscando',
    component: BuscandoComponent
  },
  {
    path: 'detectado',
    component: DetectadoComponent
  },
  {
    path: 'wifi',
    component: WifiComponent
  },
  {
    path: 'password',
    component: PasswordComponent
  },
  {
    path: 'nombre',
    component: NombreComponent
  },
  {
    path: 'anadida',
    component: AnadidaComponent
  },
  {
    path: 'dispositivo',
    component: DispositivoComponent
  },
  {
    path: 'configuracion',
    component: ConfiguracionComponent
  },
  {
    path: 'detalles-usuario',
    component: DetallesUsuarioComponent
  },
  {
    path: 'soporte',
    component: SoporteComponent
  },
];
