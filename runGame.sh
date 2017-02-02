#!/bin/bash

javac MyBot.java
javac MyBot_Medium2.java
./halite -d "50 50" "java MyBot" "java MyBot_Medium2"
