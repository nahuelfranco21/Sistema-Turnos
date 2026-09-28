# Informe de QA - Sprint 4

## 1. Introducción

Este informe detalla las actividades de prueba, resultados de cobertura y hallazgos realizados durante el **Sprint 4**. El objetivo principal fue validar las nuevas funcionalidades del sprint: reprogramación de turnos por parte del cliente, expiración automática de turnos sin pago con recordatorio por email, notificación al cliente cuando el profesional cancela, gestión de servicios por agenda, recordatorio 24 horas antes del turno, validación de agenda sin servicios, actualización de descripción de perfil del profesional, reserva sin usuario registrado y activación/desactivación de servicios. Al igual que en los sprints anteriores, se apuntó a mantener una **cobertura de instrucciones mayor al 80%** en el backend.

## 2. Preparación del Entorno de Pruebas

Se siguió el mismo flujo de construcción y ejecución utilizado en sprints anteriores:

1. **Construcción del Artefacto**: Compilación y empaquetado omitiendo los tests:
   - `./mvnw package -DskipTests`
2. **Ejecución de Pruebas con Cobertura**: Mediante JaCoCo integrado en el ciclo `verify`:
   - `./mvnw verify`
3. **Reporte de Cobertura**: Generado en formato HTML y CSV en:
   - `backend/target/site/jacoco/index.html`

## 3. Resumen de Cobertura

| Métrica | Resultado |
| :--- | :--- |
| **Cobertura de Instrucciones** | **81.7%** (5,489 / 6,722 instrucciones) |
| **Cobertura de Líneas** | **84.9%** (1,218 / 1,435 líneas) |
| **Cobertura de Ramas** | **58.0%** (290 / 500 ramas) |
| **Tests Backend (JUnit)** | **239 tests** (0 fallos, 0 errores) |
| **Tests Frontend (Vitest)** | **5 tests** (0 fallos, 0 errores) |
| **Total Tests** | **244 tests** |

### Cobertura por Paquete (Backend)

| Paquete | Cobertura |
| :--- | :--- |
| agenda | 88.0% |
| turno | 83.0% |
| user | 72.7% |
| user.email_verification | 65.5% |
| user.password_recovery | 100.0% |
| user.refresh_token | 100.0% |
| config.security | 95.1% |
| reserva | 100.0% |
| servicio | 97.5% |
| email | 100.0% |
| notificacion | 100.0% |
| common.exception | 100.0% |
| **Total general** | **81.7%** |

## 4. Nuevas Funcionalidades Probadas

### A. Reprogramación de Turnos ( `PUT /turno/{id}/reprogramar` )

El sprint 4 introdujo un flujo nuevo: cuando el profesional cancela un turno que ya estaba confirmado, este pasa a estado `REPROGRAMAR` en lugar de cancelarse directamente. Esto le da al cliente la posibilidad de elegir un nuevo horario sin perder la seña. Se testearon tanto el camino feliz como los casos de borde:

- Que el cliente pueda reprogramar correctamente y el turno quede en `CONFIRMADO` con el nuevo slot.
- Que si el turno no está en estado `REPROGRAMAR` (por ejemplo, está `CONFIRMADO`), la operación falle con `400 Bad Request`.
- Que otro cliente no pueda reprogramar un turno que no es suyo (`403 Forbidden`).

### B. Expiración Automática y Recordatorio de Pago ( `TurnoService` )

Esta fue la funcionalidad más interesante de testear porque involucra lógica temporal y un scheduler. Para no depender de tiempo real en los tests unitarios, se optó por mockear el repositorio y controlar directamente qué turnos "devuelve" la consulta, evitando tener que manipular `LocalDateTime.now()`.

Se validó que los turnos en estado `OCUPADO_SIN_CONFIRMAR` con más de 24 horas de antigüedad se eliminen y se notifique al cliente. También se verificó el recordatorio previo (a las 23 horas): que se envíe el mail, que el turno quede marcado con `reminderSent = true`, y que no se vuelva a enviar si ya fue marcado. En todos los casos también se chequeó el escenario vacío para confirmar que no se dispara ninguna notificación innecesariamente.

### C. Recordatorio 24 Horas Antes del Turno ( `enviarRecordatorios24Hs` )

El sprint agregó un nuevo método de scheduler que envía una notificación a los clientes que tienen un turno confirmado para el día siguiente. El desafío de testearlo está en la dependencia temporal: el método construye la fecha de mañana usando `ZoneId.of("America/Argentina/Buenos_Aires")`, por lo que los tests unitarios tienen que alinearse con esa misma zona para no generar falsos negativos según en qué hora del día corre la suite.

Se cubrieron los dos caminos esperados: el caso vacío (ningún turno para mañana, el método devuelve cero sin invocar nada) y el caso con un turno válido (se llama a `onRecordatorio24Hs`, el flag `recordatorio24hEnviado` queda en `true` y el turno se persiste). Adicionalmente, se testearon en `EmailServiceTest` los tres aspectos del mensaje: destinatario, presencia del nombre del cliente y del profesional en el cuerpo, y el asunto esperado. Lo mismo en `NotificacionServiceTest` para verificar que la capa de notificaciones delega y maneja bien los casos nulos o excepciones.

### D. Validación de Agenda sin Servicios

Se agregó una validación en el flujo de creación de turno: si la agenda del profesional no tiene servicios configurados, el sistema rechaza la reserva con `400 Bad Request`. Esta validación tiene sentido porque sin servicios no hay precio ni descripción sobre qué se está reservando. Se la cubrió tanto a nivel unitario (mockeando `servicioRepository.findByAgenda_Id` para que devuelva lista vacía) como a nivel de integración con MockMvc (creando una agenda sin servicios en la BD de prueba y verificando el status 400).

### E. Reserva sin Usuario Registrado ( `nombreCliente` )

El sprint incorporó la posibilidad de que el profesional registre un turno en nombre de una persona externa no registrada en el sistema, simplemente indicando su nombre mediante el campo `nombreCliente`. En ese caso el turno pasa directamente a estado `CONFIRMADO` sin requerir que exista un usuario en la base de datos.

Se cubrió esta funcionalidad tanto a nivel unitario (mockeando `turnoRepository.findByAgendaIdAndFecha` para simular slot libre, verificando que el resultado tenga `nombreCliente` correcto y que `save` sea invocado) como a nivel de integración con MockMvc (enviando el campo `nombreCliente` en el body del POST `/turno` con token de profesional y validando `201 Created` con el campo en la respuesta).

### F. Gestión de Servicios ( `/servicio/**` )

Se creó un archivo de tests de integración completo (`ServicioRestControllerTest.java`) para el módulo de servicios, que era completamente nuevo en este sprint. Se cubrieron los tres endpoints CRUD y el nuevo endpoint de toggle:

- `GET /servicio/agenda/{id}`: devuelve la lista de servicios de una agenda, o `404` si la agenda no existe.
- `POST /servicio`: solo el profesional dueño puede crear servicios; tanto el cliente como un profesional ajeno reciben `403`.
- `DELETE /servicio/{id}`: mismo control de permisos que el alta, más el `404` para ids inexistentes.
- `PATCH /servicio/{id}/toggle-activo`: permite al profesional dueño activar/desactivar un servicio. Se verificaron los tres escenarios de error: cliente (`403`), profesional ajeno (`403`) y servicio inexistente (`404`).

### G. Actualización de Descripción de Perfil ( `PUT /users/descripcion` )

Se incorporó un nuevo endpoint que permite a los profesionales actualizar la descripción de su perfil. Se agregaron tests en dos niveles: a nivel de servicio (unitario con Mockito) para verificar que `updateDescripcion` invoca correctamente `userRepository.save` y lanza `UsernameNotFoundException` para usuarios inexistentes; y a nivel de controlador (integración con MockMvc) para verificar que el endpoint responde `200 OK` con token válido y `403` sin autenticación.

### H. Notificaciones por Email ( `NotificacionService` y `EmailService` )

La cobertura inicial de estos dos módulos era baja (33% y 45% respectivamente) porque dependen de un servidor SMTP real para funcionar. La solución fue mockear `JavaMailSender` con Mockito y usar `ArgumentCaptor<SimpleMailMessage>` para capturar los mensajes enviados y verificar su contenido sin necesitar conectividad real. Esto permitió testear todos los métodos de `EmailService` verificando destinatario, asunto y contenido del cuerpo, y testear `NotificacionService` verificando que delega correctamente y que maneja bien los casos nulos o excepciones sin romper el flujo. Ambos módulos cerraron el sprint con **100% de cobertura de instrucciones**.

## 5. Archivos de Test Agregados/Modificados

| Archivo | Tipo | Tests agregados |
| :--- | :--- | :--- |
| `TurnoServiceTest.java` | Unitario (Mockito) | 11 |
| `TurnoRestControllerTest.java` | Integración (MockMvc) | 5 |
| `ServicioRestControllerTest.java` | Integración (MockMvc) | 13 (archivo nuevo) |
| `NotificacionServiceTest.java` | Unitario (Mockito) | 16 (archivo nuevo) |
| `EmailServiceTest.java` | Unitario (Mockito) | 16 (archivo nuevo) |
| `UserServiceTest.java` | Unitario (Mockito) | 2 |
| `UserRestControllerTest.java` | Integración (MockMvc) | 2 |

## 6. Resumen de Resultados

| Categoría | Estado | Observaciones |
| :--- | :--- | :--- |
| **Reprogramación de Turnos** | **Aprobado** | Flujo REPROGRAMAR → CONFIRMADO correcto, permisos validados. |
| **Expiración Sin Pago** | **Aprobado** | Scheduler elimina y notifica correctamente. |
| **Recordatorio de Pago** | **Aprobado** | Mail enviado en ventana de 23h, flag `reminderSent` actualizado. |
| **Recordatorio 24 Horas** | **Aprobado** | Scheduler envía aviso y marca flag `recordatorio24hEnviado`. |
| **Validación sin Servicios** | **Aprobado** | `400 Bad Request` al intentar reservar agenda sin servicios. |
| **Reserva sin Usuario** | **Aprobado** | Turno con `nombreCliente` creado en estado `CONFIRMADO` correctamente. |
| **Toggle Activo de Servicio** | **Aprobado** | `PATCH /servicio/{id}/toggle-activo` con control de permisos verificado. |
| **Gestión de Servicios** | **Aprobado** | CRUD completo con control de permisos verificado. |
| **Actualización de Descripción** | **Aprobado** | `PUT /users/descripcion` con autenticación y sin autenticación cubiertos. |
| **Notificaciones por Email** | **Aprobado** | `EmailService` y `NotificacionService` al 100% de cobertura. |
| **Cobertura de Código** | **Objetivo cumplido** | 81.7% de instrucciones superando el 80% objetivo. |

## 7. Decisiones de Implementación de QA

Se continuó con la estrategia de sprints anteriores combinando tests de integración con MockMvc y tests unitarios con Mockito. El criterio general es usar integración para los endpoints REST (donde importa validar autenticación, autorización y persistencia en H2) y unitarios para la lógica de servicio donde es más fácil controlar el estado.

La decisión más relevante de este sprint fue cómo encarar la cobertura de `EmailService`. Una opción era levantar un servidor SMTP de prueba (como GreenMail), pero eso agrega complejidad al setup del entorno de test. Se optó por mockear directamente `JavaMailSender` con Mockito, lo que resultó suficiente para verificar que los métodos construyen y envían los mensajes correctamente, incluyendo destinatario, asunto y contenido. Es una solución más liviana y que encaja bien con el resto del enfoque de tests del proyecto.

En cuanto a los tests temporales para `enviarRecordatorios24Hs`, se eligió no mockear el reloj del sistema sino construir los objetos de prueba usando la misma zona horaria que usa el servicio (`America/Argentina/Buenos_Aires`). Esto hace que los tests sean más representativos del comportamiento real, aunque introduce una dependencia horaria que podría dar problemas en edge cases muy específicos (por ejemplo, tests corriendo exactamente a medianoche). Para el nivel de madurez del proyecto es una compensación razonable.

## 8. Conclusión

El backend del sistema alcanzó un **81.7% de cobertura de instrucciones** con **239 tests**, superando el objetivo del 80%. Lo más destacado del sprint en términos de QA fue la cobertura completa de los módulos de email y notificaciones (ambos al 100%), que venían sin tests desde su incorporación en el sprint anterior. También se sumaron tests para todas las funcionalidades nuevas del sprint: el flujo de reprogramación, el scheduler de recordatorios 24hs, la validación de agenda sin servicios, el endpoint de descripción de perfil, la reserva con `nombreCliente` para usuarios externos, y el toggle de activación/desactivación de servicios. No se detectaron bugs durante las pruebas; todos los comportamientos fueron los esperados.
