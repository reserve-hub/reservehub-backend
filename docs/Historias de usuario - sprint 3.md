### **HU-10: Cancelación y reagendamiento de reservas**

Como cliente autenticado,  
 quiero cancelar o reagendar una reserva existente,  
 para modificar mi cita cuando no pueda asistir en el horario inicialmente seleccionado.

## **Criterios de aceptación**

### **Escenario 1: Cancelación exitosa de una reserva activa**

* El sistema debe permitir cancelar una reserva cuando el usuario autenticado sea el propietario de la reserva.  
* La reserva debe cambiar su estado a **cancelada**.  
* El cupo asociado a la franja horaria debe liberarse nuevamente.  
* El sistema debe registrar la fecha y hora de la cancelación.  
* El sistema debe retornar una respuesta exitosa indicando que la reserva fue cancelada.

### **Escenario 2: Rechazo de cancelación de una reserva inexistente**

* Si el usuario intenta cancelar una reserva que no existe, el sistema debe rechazar la operación.  
* El sistema debe retornar un mensaje indicando que la reserva no fue encontrada.  
* No debe modificarse ninguna disponibilidad ni registro existente.

### **Escenario 3: Rechazo de cancelación por usuario no autorizado**

* Si un usuario intenta cancelar una reserva que no le pertenece, el sistema debe rechazar la operación.  
* El sistema debe retornar una respuesta de acceso no autorizado o permisos insuficientes.  
* El estado de la reserva debe permanecer sin cambios.

### **Escenario 4: Reagendamiento exitoso de una reserva**

* El sistema debe permitir reagendar una reserva activa cuando el usuario autenticado sea el propietario.  
* El nuevo horario seleccionado debe existir, estar activo y tener cupos disponibles.  
* La reserva debe actualizarse con la nueva franja horaria.  
* El cupo de la franja anterior debe liberarse.  
* El cupo de la nueva franja debe descontarse.  
* El sistema debe retornar una respuesta exitosa indicando que la reserva fue reagendada.

### **Escenario 5: Rechazo de reagendamiento por falta de disponibilidad**

* Si el nuevo horario no tiene cupos disponibles, el sistema debe rechazar el reagendamiento.  
* La reserva debe conservar su horario original.  
* No debe modificarse la disponibilidad de la franja anterior ni de la nueva.  
* El sistema debe retornar un mensaje indicando que el horario no está disponible.

### **Escenario 6: Rechazo de reagendamiento de reserva cancelada**

* Si la reserva ya se encuentra cancelada, el sistema no debe permitir reagendarla.  
* El sistema debe retornar un mensaje indicando que una reserva cancelada no puede ser modificada.  
* La información de la reserva debe permanecer sin cambios.

### **HU-11: Consulta de historial de reservas**

Como usuario autenticado,  
 quiero consultar el historial de mis reservas,  
 para revisar el estado y detalle de mis citas realizadas, activas, canceladas o modificadas.

## **Criterios de aceptación**

### **Escenario 1: Consulta exitosa del historial del cliente**

* El sistema debe permitir que un cliente autenticado consulte sus reservas.  
* La respuesta debe incluir reservas activas, finalizadas, canceladas o reagendadas.  
* Cada reserva debe mostrar información básica como servicio, proveedor, fecha, hora y estado.  
* El sistema solo debe retornar reservas asociadas al usuario autenticado.

### **Escenario 2: Consulta de historial sin reservas registradas**

* Si el usuario autenticado no tiene reservas registradas, el sistema debe retornar una respuesta válida.  
* La respuesta debe indicar que no existen reservas asociadas al usuario.  
* El sistema no debe retornar error por ausencia de datos.

### **Escenario 3: Filtrado del historial por estado**

* El sistema debe permitir filtrar reservas por estado.  
* Los resultados deben coincidir únicamente con el estado solicitado.  
* Si no hay reservas con ese estado, el sistema debe retornar una lista vacía o mensaje informativo.

### **Escenario 4: Filtrado del historial por rango de fechas**

* El sistema debe permitir consultar reservas dentro de un rango de fechas.  
* Solo deben retornarse reservas cuya fecha esté dentro del rango indicado.  
* Si el rango de fechas es inválido, el sistema debe rechazar la consulta.

### **Escenario 5: Restricción de acceso al historial de otro usuario**

* Un usuario autenticado no debe poder consultar reservas de otro usuario.  
* Si intenta hacerlo, el sistema debe denegar la solicitud.  
* El sistema debe retornar una respuesta de permisos insuficientes.

### **Escenario 6: Consulta de reservas por proveedor**

* El sistema debe permitir que un proveedor autenticado consulte las reservas asociadas a sus servicios o agenda.  
* La respuesta debe incluir únicamente reservas correspondientes al proveedor autenticado.  
* El proveedor no debe poder consultar reservas asociadas a otros proveedores.

### **HU-12: Consulta de reportes operativos**

Como administrador o proveedor,  
 quiero consultar reportes operativos sobre reservas y ocupación,  
 para analizar el uso de los servicios y la disponibilidad de recursos.

## **Criterios de aceptación**

### **Escenario 1: Consulta exitosa de reporte por administrador**

* El sistema debe permitir que un administrador autenticado consulte reportes generales de reservas.  
* El reporte debe incluir información como total de reservas, reservas canceladas, reservas activas y reservas finalizadas.  
* El sistema debe permitir consultar la información por rango de fechas.  
* La respuesta debe retornar datos estructurados para ser consumidos por otro sistema o cliente.

### **Escenario 2: Consulta de reporte por proveedor**

* El sistema debe permitir que un proveedor autenticado consulte reportes relacionados con sus propios servicios.  
* El reporte debe mostrar información asociada únicamente a las reservas del proveedor autenticado.  
* El proveedor no debe poder consultar reportes de otros proveedores.

### **Escenario 3: Reporte de ocupación de servicios**

* El sistema debe calcular la ocupación de los servicios según los cupos disponibles y las reservas realizadas.  
* El reporte debe permitir identificar servicios con mayor o menor uso.  
* Los datos deben calcularse con base en las reservas registradas en el sistema.

### **Escenario 4: Consulta sin datos en el periodo solicitado**

* Si no existen reservas en el rango de fechas consultado, el sistema debe retornar una respuesta válida.  
* La respuesta debe indicar que no hay información disponible para el periodo.  
* El sistema no debe generar error por ausencia de datos.

### **Escenario 5: Rechazo por rol no autorizado**

* Si un cliente intenta consultar reportes operativos, el sistema debe rechazar la solicitud.  
* El sistema debe retornar una respuesta de permisos insuficientes.  
* No debe exponerse información operativa del sistema.

### **Escenario 6: Validación de rango de fechas**

* Si el rango de fechas enviado es inválido, el sistema debe rechazar la consulta.  
* El sistema debe retornar un mensaje indicando que el rango de fechas no es válido.  
* No debe generarse ningún reporte con fechas incorrectas.

