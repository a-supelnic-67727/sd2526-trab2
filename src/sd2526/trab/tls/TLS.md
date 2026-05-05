# Para o server:

keytool -ext SAN=dns:<server-name> -genkeypair -alias <server-name> keyalg RSA -validity 365 -keystore <keystore-filename> -storetype pkcs12

keytool -ext SAN=dns:messages.ourorg2 -genkeypair -alias messages.ourorg2 -keyalg RSA -validity 365 -keystore messages.ourorg2-server.ks -storetype pkcs12

Quando pedir password - digitar "password"

# Para o client:

copy "C:\Program Files\Java\jdk-17\lib\security\cacerts" truststore.ks

# Para exportar o server certificate:

keytool -exportcert -alias <server-name> -keystore <keystore-file> -file <certificate-file>

keytool -exportcert -alias users.domain -keystore users-domain-server.ks -file users-domain.cert

# Now that we have exported the server certificate, we just need to add it to the client truststore:

keytool -importcert -file <certificate-file> -alias <server-name> -keystore <keystore-file>

keytool -importcert -file users-domain.cert -alias users.domain -keystore truststore.ks

Quando pedir password - digitar "changeit"

# Fazer então alterações no AbstractRestServer e no GrpcUsersServer

# Meter no Dockerfile:

## EXECUTING THE SERVERS

  When the servers execute, they must know what is the file 
  that contains their keystore and the password for that 
  keystore.
  For clients, and for servers that also execute requests to 
  other servers, we must also know the file containing the 
  truststore file and its password.
  This information is conveyed through arguments to the java 
  machine:

### Exemplo: 

  This command is for the 
  gRPC server in a docker 
  named users.

  (Notice that the example 
in this week executes the 
gRPC Server as default, 
also providing the jvm 
variables for the keystore 
and truststore)

  CMD ["java", "-cp", "sd2526.jar", "-Djavax.net.ssl.keyStore=/home/sd/users-domain-server.ks" ,\
								"-Djavax.net.ssl.keyStorePassword=password" ,\
								"-Djavax.net.ssl.trustStore=/home/sd/truststore.ks" ,\
								"-Djavax.net.ssl.trustStorePassword=changeit" ,\
								"lab8.impl.server.grpc.UsersServer"]

java -Djavax.net.ssl.keyStore=<keystore-filename> -Djavax.net.ssl.keyStorePassword=<keystore-password> -Djavax.net.ssl.trustStore=<truststore-filename>-Djavax.net.ssl.trustStorePassword=<truststore-password>
<mainClass>

## copy keystore and truststore
COPY *.ks /home/sd/

# RestClient não muda nada, no entanto o GrpcClient necessita de alterações

# EXECUTING THE CLIENTS

When the clients execute, they must know what is the file 
that contains the truststore and the password for that 
truststore.
Similar to the servers this information is conveyed through 
arguments to the java machine (but only those that refer to 
the truststore):

This is the example to execute the 
client that creates Users (it can 
interact with both REST and gRPC 
servers) in a docker container on the 
same network as the server.

java -cp sd2526.jar -Djavax.net.ssl.trustStore=truststore.ks-Djavax.net.ssl.trustStorePassword=changeit lab8.clients.CreateUserClient 
