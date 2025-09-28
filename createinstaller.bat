//This is not a working script.

jlink
--module-path "D:\ProgramFiles\jdk-21.0.2\jmods;D:\ProgramFiles\javafx-jmods-21.0.7"
--add-modules java.base,java.desktop,java.logging,java.sql,java.xml,java.management,jdk.unsupported,javafx.controls,javafx.fxml,javafx.base,javafx.graphics
--output custom-runtime

jpackage
--type exe
--input .
--dest .
--main-jar ServiceLetterGenerator.jar
--main-class com.dumindut.servicelettergenerator.App
--runtime-image custom-runtime
--win-shortcut
--win-menu
--name "ServiceLetterGenerator"
--icon "D:\Personal\Projects\ServiceLetterGenerator\src\main\resources\images\mainicon.ico"

=================

jlink --module-path "D:\ProgramFiles\jdk-21.0.2\jmods;D:\ProgramFiles\javafx-jmods-21.0.7" --add-modules java.base,java.desktop,java.logging,java.sql,java.xml,java.management,jdk.unsupported,javafx.controls,javafx.fxml,javafx.base,javafx.graphics,jdk.charsets --output custom-runtime
jpackage --type exe --input . --dest . --main-jar ServiceLetterGenerator.jar --main-class com.dumindut.servicelettergenerator.App --runtime-image custom-runtime --win-shortcut --win-menu --name "ServiceLetterGenerator" --icon "D:\Personal\Projects\ServiceLetterGenerator\src\main\resources\images\mainicon.ico" --app-version "1.2" --vendor "J.G.D.A. Thilakaratne"
