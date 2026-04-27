**HU-06 \- Definir agenda u horarios disponibles**  
**Como** proveedor de servicios  
**Quiero** registrar franjas de disponibilidad en mi agenda  
**Para** ofrecer horarios reservables a los clientes.

Qué debería incluir:

* crear bloques de horario por fecha, día o franja  
* definir duración del servicio o intervalo  
* indicar cupos disponibles por horario  
* evitar crear horarios inválidos o traslapados  
* permitir activar o desactivar franjas

### **Criterios de aceptación**

**Escenario 1: Registro exitoso de una franja disponible**

* El proveedor autenticado accede al módulo de agenda.  
* Registra una franja indicando fecha, hora de inicio, hora de fin y cupo disponible.  
* El sistema valida que los datos sean correctos.  
* La franja se guarda correctamente.  
* La franja queda disponible para consulta y reserva por parte de los clientes.

**Escenario 2: Rango horario inválido**

* El proveedor intenta registrar una franja cuya hora final es menor o igual a la hora inicial.  
* El sistema rechaza el registro.  
* El sistema muestra un mensaje indicando que el rango horario no es válido.

**Escenario 3: Franja traslapada**

* El proveedor intenta registrar una franja que se cruza con otra ya existente para la misma agenda.  
* El sistema detecta el conflicto.  
* El sistema no guarda la franja.  
* El sistema muestra un mensaje indicando que existe traslape de horarios.

**Escenario 4: Campos obligatorios incompletos**

* El proveedor intenta guardar la franja sin completar uno o más campos requeridos.  
* El sistema muestra validaciones en cliente.  
* No se envía la solicitud al servidor.

**Escenario 5: Activación o desactivación de franja**

* El proveedor accede a una franja previamente registrada.  
* Puede cambiar su estado a activa o inactiva.  
* Si la franja queda inactiva, no debe mostrarse como disponible para nuevas reservas.

**HU-07 \- Consultar disponibilidad de servicios**  
 **Como** cliente  
 **Quiero** consultar los servicios y sus horarios disponibles  
 **Para** elegir una fecha y hora para mi reserva

Qué debería incluir:

* listar servicios disponibles  
* filtrar por proveedor, fecha o tipo de servicio  
* mostrar solo horarios realmente disponibles  
* indicar cuando un servicio no tiene cupos  
* mostrar mensaje si no hay disponibilidad

### **Criterios de aceptación**

**Escenario 1: Consulta exitosa de servicios disponibles**

* El cliente accede al módulo de consulta.  
* El sistema muestra los servicios disponibles junto con sus horarios reservables.  
* Solo se muestran franjas activas y con disponibilidad.

**Escenario 2: Filtrado por criterios**

* El cliente puede filtrar por proveedor, fecha o tipo de servicio.  
* El sistema actualiza el listado según el filtro aplicado.  
* Solo se muestran resultados que cumplan con los criterios ingresados.

**Escenario 3: Servicio sin cupos**

* Si un servicio o franja no tiene cupos disponibles, el sistema lo indica claramente.  
* El cliente no debe poder seleccionarlo para reservar.

**Escenario 4: Sin resultados disponibles**

* Si no existen servicios u horarios disponibles según la búsqueda realizada, el sistema muestra un mensaje informativo.  
* No se presentan errores de carga.

### **HU-08 \- Crear una reserva**

**Como** cliente autenticado  
**Quiero** crear una reserva en un horario disponible   
**Para** asegurar mi cita en la plataforma.

Qué debería incluir:

* seleccionar servicio, proveedor, fecha y hora  
* validar disponibilidad al momento de confirmar  
* registrar la reserva  
* descontar un cupo disponible  
* mostrar confirmación de reserva

### **Criterios de aceptación**

**Escenario 1: Creación exitosa de reserva**

* El cliente autenticado selecciona servicio, proveedor, fecha y hora disponibles.  
* El sistema valida la disponibilidad al momento de confirmar.  
* El sistema registra la reserva correctamente.  
* El sistema descuenta un cupo de la franja seleccionada.  
* El sistema muestra mensaje de confirmación.

**Escenario 2: Horario sin disponibilidad**

* El cliente intenta reservar una franja sin cupos disponibles.  
* El sistema rechaza la operación.  
* El sistema informa que el horario ya no está disponible.

**Escenario 3: Horario inválido o inexistente**

* El cliente intenta reservar una franja que no existe o que ya no está activa.  
* El sistema no registra la reserva.  
* El sistema muestra un mensaje de error.

**Escenario 4: Usuario no autenticado**

* Un usuario sin sesión intenta crear una reserva.  
* El sistema no permite la operación.  
* El sistema redirige al inicio de sesión o solicita autenticación.

**Escenario 5: Confirmación y actualización de disponibilidad**

* Luego de una reserva exitosa, la disponibilidad visible debe actualizarse.  
* Si el cupo llega a cero, la franja debe dejar de estar reservable.

**HU-09 \- Gestionar códigos de proveedor**

**Como** administrador de la plataforma

**Quiero** gestionar códigos de proveedor

**Para** controlar qué usuarios pueden registrarse como proveedores autorizados.

Que debe de incluir**:** 

- generar código  
- listar códigos  
- ver estado  
- desactivar códigos no usados  
- impedir uso de códigos inactivos o ya consumidos

### **Criterios de aceptación**

**Escenario 1: Generación exitosa de código**

* El administrador autenticado accede al módulo de gestión de códigos.  
* Genera un nuevo código de proveedor.  
* El sistema guarda el código con estado activo y no utilizado.  
* El código queda disponible para ser usado en el registro de proveedores.

**Escenario 2: Consulta de códigos registrados**

* El administrador puede visualizar el listado de códigos generados.  
* Cada código muestra al menos su valor, estado y condición de uso.

**Escenario 3: Desactivación de código no utilizado**

* El administrador selecciona un código activo que no ha sido usado.  
* El sistema permite desactivarlo.  
* El código desactivado no puede utilizarse en nuevos registros.

**Escenario 4: Restricción sobre códigos consumidos**

* Si un código ya fue usado en un registro exitoso, el sistema lo marca como consumido.  
* El código no puede reutilizarse para registrar otro proveedor.

**Escenario 5: Acceso restringido al módulo**

* Si un usuario sin rol de administrador intenta acceder al módulo de códigos, el sistema niega el acceso.