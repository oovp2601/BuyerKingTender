#!/bin/bash
echo "=================================="
echo "   BUYERKING TENDER SYSTEM"
echo "=================================="
echo ""

# Kill any existing server on port 8080
echo "Stopping any running server..."
pkill -f "buyerking.Main" 2>/dev/null
PID_ON_PORT=$(lsof -ti:8080 2>/dev/null)
if [ -n "$PID_ON_PORT" ]; then
    kill -9 "$PID_ON_PORT" 2>/dev/null
fi
sleep 1
echo "Port 8080 is free."
echo ""

# Compile Java files
echo "Compiling Java files..."
javac -d bin -cp "lib/mysql-connector-j-8.3.0.jar:lib/sqlite-jdbc-3.41.2.2.jar:lib/javax.servlet-api-4.0.1.jar" src/buyerking/*.java src/buyerking/web/*.java

if [ $? -ne 0 ]; then
    echo "Compilation failed!"
    exit 1
fi

# Run the program
echo "Running BuyerKing System..."
echo ""
java -cp "bin:lib/mysql-connector-j-8.3.0.jar:lib/sqlite-jdbc-3.41.2.2.jar:lib/javax.servlet-api-4.0.1.jar" buyerking.Main