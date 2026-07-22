import { Component, OnInit, AfterViewInit, OnDestroy, inject, ElementRef, ViewChild } from '@angular/core';
import { IonicModule } from '@ionic/angular';
import { CommonModule } from '@angular/common';
import { Location, DeviceCoordinates } from '../../services/location';
import { Bluetooth } from '../../services/bluetooth';
import * as L from 'leaflet';

@Component({
  selector: 'app-mapa',
  templateUrl: './mapa.component.html',
  styleUrls: ['./mapa.component.scss'],
  standalone: true,
  imports: [IonicModule, CommonModule]
})
export class MapaComponent implements OnInit, AfterViewInit, OnDestroy {
  public locationService = inject(Location);
  public bluetoothService = inject(Bluetooth);

  private map: L.Map | null = null;
  private marker: L.Marker | null = null;
  private accuracyCircle: L.Circle | null = null;

  // Coordenadas por defecto (ej. CDMX u origen genérico si no hay GPS activado aún)
  public lastCoords: DeviceCoordinates | null = null;

  ngOnInit() {}

  ngAfterViewInit() {
    this.initMap();
    this.obtenerUbicacionActual();
  }

  ngOnDestroy() {
    this.locationService.stopWatch();
    if (this.map) {
      this.map.remove();
      this.map = null;
    }
  }

  private initMap() {
    const initialLat = 19.432608;
    const initialLng = -99.133209;

    this.map = L.map('map-container', {
      center: [initialLat, initialLng],
      zoom: 16,
      zoomControl: false
    });

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap'
    }).addTo(this.map);

    // Ajuste de tamaño al renderizar
    setTimeout(() => {
      if (this.map) this.map.invalidateSize();
    }, 400);
  }

  async obtenerUbicacionActual() {
    const coords = await this.locationService.getCurrentPosition();
    if (coords) {
      this.actualizarMarcadorUbicacion(coords);
    }

    // Iniciar seguimiento en tiempo real
    this.locationService.startWatch((updatedCoords) => {
      this.actualizarMarcadorUbicacion(updatedCoords);
    });
  }

  actualizarMarcadorUbicacion(coords: DeviceCoordinates) {
    this.lastCoords = coords;
    if (!this.map) return;

    const latLng = L.latLng(coords.lat, coords.lng);
    const selectedDev = this.bluetoothService.selectedDevice();
    const deviceName = selectedDev?.name || 'Safeband / Dispositivo';
    const deviceId = selectedDev?.deviceId || 'Dispositivo Principal';

    // Icono personalizado SVG para el pin del dispositivo
    const customIcon = L.divIcon({
      className: 'custom-map-marker',
      html: `
        <div class="marker-pulse-wrapper">
          <div class="marker-pulse"></div>
          <div class="marker-icon-box">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7z"/>
              <circle cx="12" cy="9" r="2.5"/>
            </svg>
          </div>
        </div>
      `,
      iconSize: [40, 40],
      iconAnchor: [20, 20],
      popupAnchor: [0, -20]
    });

    if (this.marker) {
      this.marker.setLatLng(latLng);
    } else {
      this.marker = L.marker(latLng, { icon: customIcon }).addTo(this.map);
    }

    const popupContent = `
      <div style="font-family: sans-serif; padding: 4px;">
        <strong style="color: #0E9AA7; font-size: 14px;">📍 ${deviceName}</strong><br/>
        <span style="font-size: 11px; color: #666;">ID: ${deviceId}</span><br/>
        <span style="font-size: 11px; color: #444;">Lat: ${coords.lat.toFixed(5)}, Lng: ${coords.lng.toFixed(5)}</span>
      </div>
    `;

    this.marker.bindPopup(popupContent);

    // Círculo de precisión
    if (coords.accuracy) {
      if (this.accuracyCircle) {
        this.accuracyCircle.setLatLng(latLng);
        this.accuracyCircle.setRadius(coords.accuracy);
      } else {
        this.accuracyCircle = L.circle(latLng, {
          radius: coords.accuracy,
          color: '#0E9AA7',
          fillColor: '#0E9AA7',
          fillOpacity: 0.15,
          weight: 1
        }).addTo(this.map);
      }
    }

    this.map.setView(latLng, 16);
  }

  centrarMapa() {
    if (this.lastCoords && this.map) {
      this.map.setView([this.lastCoords.lat, this.lastCoords.lng], 16, { animate: true });
    } else {
      this.obtenerUbicacionActual();
    }
  }
}

