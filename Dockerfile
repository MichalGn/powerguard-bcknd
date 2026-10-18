#1.
#sudo docker build -t powerguard-bcknd_image .

#sudo docker save -o powerguard-bcknd-261017.tar powerguard-bcknd_image   //Zapisanie do pliku
#sudo docker load -i powerguard-bcknd-231219.tar  //rozpakowanie pliku:

#Otworzenie bazy w cmd
#docker exec -it powerguard-postgresdb_container psql -U postgres -d powerguarddb


#*******************************************************************************
#*** Przygotowanie obrazu do uruchomienia na raspberry: **
#docker buildx build --platform linux/arm64 -t powerguard-bcknd_image_arm64 --load .

#spr. na raspberry
#docker image inspect powerguard-bcknd_image_arm64 --format "{{.Architecture}}"

#docker save -o powerguard-bcknd-261017_arm64.tar powerguard-bcknd_image_arm64
#===============================================================================


#FROM quay.io/wildfly/wildfly-runtime:latest
FROM quay.io/wildfly/wildfly-runtime:latest-jdk25

# Copy application
COPY --chown=jboss:root target/server $JBOSS_HOME

# Set permissions
RUN chmod -R ug+rwX $JBOSS_HOME
