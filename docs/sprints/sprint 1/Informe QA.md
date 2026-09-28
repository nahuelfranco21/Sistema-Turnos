# Informe de QA - Sprint 1: Módulo de Usuarios y Sesiones

## 1. Introducción
Este informe detalla las actividades de prueba, decisiones técnicas y hallazgos realizados durante el **Sprint 1**. El objetivo principal fue validar la robustez del sistema de gestión de usuarios, la creación de sesiones y la seguridad de los tokens de refresco, siguiendo los lineamientos técnicos establecidos en el archivo `README.md`  del proyecto.

## 2. Preparación del Entorno de Pruebas
Para garantizar que las pruebas se ejecutaran en un entorno limpio y controlado, se siguió estrictamente el flujo de despliegue del backend:

1.  **Infraestructura de Datos**: Se levantó el contenedor de la base de datos mediante Docker:
    *   `docker-compose up -d db`
2.  **Construcción del Artefacto**: Se realizó la compilación y empaquetado del proyecto omitiendo los tests unitarios previos para centrarse en pruebas funcionales de caja negra:
    *   `mvn package -DskipTests`
3.  **Ejecución del Servidor**: Se inició la aplicación Spring Boot:
    *   `java -jar target/product-template-0.0.1-SNAPSHOT.jar`
4.  **Verificación de Interfaz**: Se validó la disponibilidad del servicio a través de la interfaz de documentación Swagger en:
    *   `http://localhost:8080/swagger-ui/index.html`

## 3. Ejecución de Pruebas Automatizadas
Las pruebas se ejecutaron mediante scripts de automatización ubicados en la carpeta `/backend/src/test/scripts/`. Se definieron dos archivos principales:

### A. Validacion de datos (`validacion_datos.sh`)
Validación del "Happy Path" y funcionalidades críticas:
*   **Registro y Login**: Creación exitosa de usuarios y obtención de tokens.
*   **Gestión de Sesiones**: Uso de `refreshToken` para renovar el `accessToken`.
*   **Seguridad de Tokens**: Verificación de la invalidación automática de tokens de refresco ya utilizados (Prevención de ataques de Replay).

### B. Pruebas de verificacion (`verificar_auth.sh`)
Detección de desviaciones y fallos de validación:
*   **Registro de usuario con formato de email inválido**.
*   **Registro con campos obligatorios vacíos** (Validación de Jakarta).
*   **Registro con fecha de nacimiento en el futuro**.
*   **Login con credenciales inexistentes o incorrectas**.
*   **Uso de tokens de refresco mal formados o aleatorios**.

## 4. Resumen de Resultados

| Categoría | Estado | Observaciones |
| :--- | :--- | :--- |
| **Funcionalidades Críticas** | **Aprobadas con observaciones** | El flujo de sesión y refresco de tokens es seguro y funcional. |
| **Seguridad/Sesiones** | **Aprobado** | La invalidación de tokens usados funciona correctamente (Error 401). |
| **Validación de Datos** | **Pendiente de mejora** | Se detectaron bugs en la lógica de registro (emails y fechas). |

### Detalle de Hallazgos (Bugs Detectados):
1.  **Falta de Validación Lógica (Fecha)**: El sistema permite el registro de usuarios con fechas de nacimiento futuras (ej. año 2030), lo cual es inconsistente para un sistema de turnos.
2.  **Debilidad en Formato de Email**: No se valida que el `username` cumpla con un formato de correo electrónico estándar, permitiendo cualquier cadena de texto.
3.  **Comportamiento de Diseño Confuso**: Al intentar registrar un usuario que ya existe con la contraseña correcta, el sistema devuelve un **201 Created** y realiza un login automático en lugar de informar un conflicto.

## 5. Decisiones de Implementación de QA
Durante este sprint, se decidió tomar una decision respecto a la implementacino de las pruebas:
*   **Cambio de Herramientas**: Originalmente se intentó la implementación de pruebas integrales dentro del entorno de Spring Boot. Debido a la complejidad que se vio en la implementacion del mismo y la necesidad de velocidad para este sprint, se optó por el uso de **Scripts de Bash (`.sh`)**.
*   **Justificación**: Los scripts permiten testear de forma directa los endpoints expuestos, garantizando que la lógica de negocio externa sea sólida. Esta solución resultó ser más "amigable" y eficiente para validar las partes críticas del sistema sin depender de la configuración interna de tests de Spring Boot, la cual presentaba fallos en esta etapa inicial del proyecto.

## 6. Conclusión
El backend presenta una base sólida en cuanto a gestión de sesiones y seguridad de tokens. Esto era necesario para lograr la verificacion de lo pedido en el primer sprint. Descartando algunos bugs por la temprana implementacion del mismo y la falta de tiempo para corregirlos, se ve como un buen avance del proyecto.  