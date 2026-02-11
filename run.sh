#!/bin/bash
echo "=================================="
echo "   BUYERKING TENDER SYSTEM"
echo "=================================="
echo ""

# Compile Java files
echo "Compiling Java files..."
javac -d bin -cp "lib/mysql-connector-j-8.3.0.jar:lib/javax.servlet-api-4.0.1.jar" src/buyerking/*.java src/buyerking/web/*.java

if [ $? -ne 0 ]; then
    echo "Compilation failed!"
    exit 1
fi

# Run the program
echo "Running BuyerKing System..."
echo ""
java -cp "bin:lib/mysql-connector-j-8.3.0.jar:lib/javax.servlet-api-4.0.1.jar" buyerking.Main