nohup /home/jdk1.8.0_121/bin/java -Djava.rmi.server.hostname=10.156.3.214 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=9021 -Dcom.sun.management.jmxremote.local.only=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -DDBGW -Dfile.encoding=UTF-8 -Xmx4120m -classpath "lib/*:lib/yaml/*" -Dlog4j.configuration=file:conf/log4j.properties org.d.sps.INGW > logs/stdout 2> logs/stderr < /dev/null &
PID=$!
echo $PID > logs/pid