#!/bin/sh -x
CLASSPATH=../craftbukkit-1.2.5-R1.0.jar:../worldguard-5.5.2.jar javac *.java -Xlint:deprecation -Xlint:unchecked
rm -rf me
mkdir -p me/exphc/EnchantMore
mv *.class me/exphc/EnchantMore/
jar cf EnchantMore.jar me/ *.yml *.java *.dat README.md ChangeLog LICENSE
