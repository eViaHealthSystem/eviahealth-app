# eViaHealth App  :heartpulse: 📲

**eViaHealth App** es la aplicación Android del sistema sanitario **eViaHealth** (software médico de clase IIa, Regla 11 – Reglamento (UE) 2017/745).  
Su propósito es permitir a pacientes y cuidadores — en domicilio o residencia — adquirir, visualizar y transmitir de forma **segura** constantes clínicas, cuestionarios y otros datos de salud al backend profesional **eViaHealth Web**.

---

## Tabla de contenidos
1. [Principales funcionalidades](#principales-funcionalidades)
2. [Arquitectura de la solución](#arquitectura-de-la-solución)
3. [Dispositivos compatibles](#dispositivos-compatibles)
4. [Requisitos y compilación](#requisitos-y-compilación)
5. [Primer arranque & flujo de configuración](#primer-arranque--flujo-de-configuración)
6. [Seguridad & protección de datos](#seguridad--protección-de-datos)
7. [Cumplimiento normativo](#cumplimiento-normativo)
8. [Guía rápida para contribuidores](#guía-rápida-para-contribuidores)
9. [Licencia](#licencia)

---

## Principales funcionalidades

| Categoría | Descripción |
|-----------|-------------|
| **Adquisición de datos** | Registro automático vía Bluetooth LE de SpO₂, tensión arterial, temperatura, peso, actividad/sueño, entrada manual para peso/temperatura/cuestionarios. |
| **Gestión de ensayo** | Wizard diario con lista secuencial de tareas médicas (mediciones & encuestas) definida por el prescriptor. |
| **Transmisión segura** | Cifrado en tránsito (TLS 1.3) y en reposo local; reintento automático cuando no hay conectividad (modo offline). |
| **Asignación unívoca** | Vínculo tablet-paciente mediante QR token; IMEI validado; bloqueo modo KIOSK. |
| **Videollamada integrada** | SDK Zoom incrustado para sesiones programadas por el clínico (no regulado como función médica). |
| **Mantenimiento técnico** | Modo técnico oculto con doble autenticación, pairing BLE guiado y trazabilidad de configuraciones. |

---

## Arquitectura de la solución

```mermaid
graph TD
  subgraph eViaHealth App Android
    ui["UI"]
    api["API Layer"]
    bluetooth["BT Manager"]
    devices["Device SDK / SOUP"]
    models["Domain Models"]
    meeting["Meeting SDK"]
    utils["Utils"]
  end
  ui --> api
  api --> bluetooth --> devices
  api --> models
  ui --> meeting
  utils --> *
