#!/bin/bash

sbt package

if [ $? != 0 ]
then
  echo "'sbt package' failed, exiting now"
  exit 1
fi

cp target/scala-2.10/weather_2.10-0.1.jar Weather.jar

ls -l Weather.jar

echo ""
echo "Created Weather.jar. Copy that file to /Users/al/Sarah/plugins/DDWeather, like this:"
echo "cp Weather.jar /Users/al/Sarah/plugins/DDWeather"

