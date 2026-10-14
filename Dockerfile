#1.
#sudo docker build -t powerguard-bcknd_image .


#sudo docker save -o powerguard-bcknd-261013.tar powerguard-bcknd_image   //Zapisanie do pliku
#sudo docker load -i powerguard-bcknd-231219.tar  //rozpakowanie pliku:

#Otworzenie bazy w cmd
#docker exec -it powerguard-postgresdb_container psql -U postgres -d powerguarddb


#FROM quay.io/wildfly/wildfly-runtime:latest
FROM quay.io/wildfly/wildfly-runtime:latest-jdk25

# Copy application
COPY --chown=jboss:root target/server $JBOSS_HOME

# Set permissions
RUN chmod -R ug+rwX $JBOSS_HOME
