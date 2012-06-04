#!/bin/bash

BACKEND_DIR="/Users/hasaneinali/MyProjects/RhinoCompanion/src/backend"
PACKAGES_DIR="/Users/hasaneinali/MyProjects/RhinoCompanion/src/bin/packages"
SIP_RA_DIR="/Users/hasaneinali/MyProjects/RhinoCompanion/src/required_RAs/sip-connectivity-2.3.0.0/sip-2.3.0.0"
HTTP_RA_DIR="/Users/hasaneinali/MyProjects/RhinoCompanion/src/required_RAs/web-connectivity-2.1.2.2/http-2.1.2"

# Compile the packages
cd ${BACKEND_DIR}
for dir in *
do
	# Package the Profile Specification
	if ( test ${dir} == "LocationDatabaseProfile")
	then
		cd ${dir}
        echo "Packaging ${dir}"
        ant clean package-profile-spec
		cd ..
		continue
	fi
  # Package the library
  if( test ${dir} == "JSON_Library")
  then
    cd ${dir}
        echo "Packaging ${dir}"
        ant clean package-library
    cd ..
    continue
  fi
	# Package the SBB
	cd $dir
    echo "Packaging ${dir}"
    ant clean package-sbb
	cd ..
done
# Create the packages directory if it is doesn't already exist
if ( test -d ${PACKAGES_DIR})
then
	rm -fr ${PACKAGES_DIR}
else
	mkdir ${PACKAGES_DIR}
fi
# Collect the packages
for dir in $(ls -ld ${BACKEND_DIR}/* | grep ^d | awk '{print $9}')
do
	echo "Entering ${dir}..."	
	cd ${dir}
		cp ./jars/*.jar ${PACKAGES_DIR}
	cd ..
done
# 
# Deploy all of the required RAs and the packages in the right order to the Rhino application server.
#
cd ${SIP_RA_DIR}
ant deploysipra
cd ${HTTP_RA_DIR}
ant deploy-httpra
cd ${PACKAGES_DIR}
/Users/hasaneinali/opt/RhinoSDK/client/bin/rhino-console installlocaldu json-java-lib-du.jar
/Users/hasaneinali/opt/RhinoSDK/client/bin/rhino-console installlocaldu location-database-profile-spec-du.jar
/Users/hasaneinali/opt/RhinoSDK/client/bin/rhino-console installlocaldu location-service-du.jar
/Users/hasaneinali/opt/RhinoSDK/client/bin/rhino-console installlocaldu service-manager-du.jar
/Users/hasaneinali/opt/RhinoSDK/client/bin/rhino-console installlocaldu sip_registration_service_du.jar
# Activate the services
/Users/hasaneinali/opt/RhinoSDK/client/bin/rhino-console activateservice name=LocationService,vendor=OpenCloud,version=1.0
/Users/hasaneinali/opt/RhinoSDK/client/bin/rhino-console activateservice name=SipRegistrationService,vendor=OpenCloud,version=1.0
/Users/hasaneinali/opt/RhinoSDK/client/bin/rhino-console activateservice name=ServiceManagementService,vendor=OpenCloud,version=1.0
