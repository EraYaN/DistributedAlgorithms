@ECHO OFF
start cmd /c java -Djava.security.manager -Djava.security.policy=src/Exercise1.policy -jar dist/Exercise1.jar 1 autostart
start cmd /c java -Djava.security.manager -Djava.security.policy=src/Exercise1.policy -jar dist/Exercise1.jar 2 autostart
rem start cmd /c java -Djava.security.manager -Djava.security.policy=src/Exercise1.policy -jar dist/Exercise1.jar 3 autostart
