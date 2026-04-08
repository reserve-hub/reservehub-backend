# =================================================================
#  Dockerfile – lab1banco
# =================================================================
#
#  Usa openjdk:17-jdk-slim en lugar de openjdk:11 porque el
#  proyecto usa Spring Boot 3 que requiere Java 17 o superior.
#  La variante "-slim" es más liviana (~200MB vs ~400MB).
#
#  Para construir la imagen localmente:
#    docker build -t lab1banco .
#
#  Para ejecutarla (necesitas una BD MySQL en 3306):
#    docker run -p 8080:8080 \
#      -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/lab1banco \
#      -e SPRING_DATASOURCE_USERNAME=root \
#      -e SPRING_DATASOURCE_PASSWORD=tu_password \
#      lab1banco
# =================================================================

# ================================================================
#  POR QUÉ SE USA eclipse-temurin:17 Y NO openjdk:10-ea:
#
#  El pom.xml compila con <java.version>17</java.version>.
#  Eso produce bytecode versión 61. Java 10 solo entiende hasta
#  versión 54 → UnsupportedClassVersionError → la app no arranca.
#  eclipse-temurin:17-jdk-alpine es la imagen oficial de OpenJDK 17
#  mantenida por Eclipse Foundation. Pesa ~180MB (muy liviana).
# ================================================================
FROM eclipse-temurin:17-jdk-alpine

EXPOSE 8080

# El nombre del JAR viene del <finalName>lab2-2026</finalName> en pom.xml
ADD target/reservehub.jar reservehub.jar

# Las variables de entorno para la BD se inyectan desde Render
# sin necesidad de reconstruir la imagen
ENTRYPOINT ["java", "-jar", "/reservehub.jar"]