#!/bin/bash
# Dosevia App - Complete Installation and Build Script for Linux/Mac
# This script will install all dependencies and build the app

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo -e "${CYAN}=====================================${NC}"
echo -e "${CYAN}Dosevia App - Installation & Build${NC}"
echo -e "${CYAN}=====================================${NC}"
echo ""

# Check if Node.js is installed
echo -e "${YELLOW}Checking Node.js installation...${NC}"
if command -v node &> /dev/null; then
    NODE_VERSION=$(node --version)
    echo -e "${GREEN}✓ Node.js version: $NODE_VERSION${NC}"
else
    echo -e "${RED}✗ Node.js is not installed!${NC}"
    echo -e "${RED}Please install Node.js from https://nodejs.org/${NC}"
    exit 1
fi

# Check if npm is installed
echo -e "${YELLOW}Checking npm installation...${NC}"
if command -v npm &> /dev/null; then
    NPM_VERSION=$(npm --version)
    echo -e "${GREEN}✓ npm version: $NPM_VERSION${NC}"
else
    echo -e "${RED}✗ npm is not installed!${NC}"
    exit 1
fi

echo ""
echo -e "${CYAN}=====================================${NC}"
echo -e "${CYAN}Step 1: Cleaning old dependencies${NC}"
echo -e "${CYAN}=====================================${NC}"

# Remove old node_modules and lock files
if [ -d "node_modules" ]; then
    echo -e "${YELLOW}Removing old node_modules...${NC}"
    rm -rf node_modules
    echo -e "${GREEN}✓ Removed old node_modules${NC}"
fi

if [ -f "package-lock.json" ]; then
    echo -e "${YELLOW}Removing old package-lock.json...${NC}"
    rm -f package-lock.json
    echo -e "${GREEN}✓ Removed old package-lock.json${NC}"
fi

echo ""
echo -e "${CYAN}=====================================${NC}"
echo -e "${CYAN}Step 2: Installing dependencies${NC}"
echo -e "${CYAN}=====================================${NC}"
echo -e "${YELLOW}This may take several minutes...${NC}"
echo ""

# Install dependencies with legacy peer deps flag to avoid conflicts
npm install --legacy-peer-deps

if [ $? -ne 0 ]; then
    echo ""
    echo -e "${RED}✗ Failed to install dependencies!${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}✓ All dependencies installed successfully!${NC}"

echo ""
echo -e "${CYAN}=====================================${NC}"
echo -e "${CYAN}Step 3: Building the application${NC}"
echo -e "${CYAN}=====================================${NC}"
echo ""

# Build the application
npm run build

if [ $? -ne 0 ]; then
    echo ""
    echo -e "${RED}✗ Build failed!${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}✓ Build completed successfully!${NC}"

echo ""
echo -e "${CYAN}=====================================${NC}"
echo -e "${CYAN}Step 4: Syncing with Capacitor${NC}"
echo -e "${CYAN}=====================================${NC}"
echo ""

# Sync with Capacitor Android
npx cap sync android

if [ $? -ne 0 ]; then
    echo ""
    echo -e "${RED}✗ Capacitor sync failed!${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}✓ Capacitor sync completed successfully!${NC}"

echo ""
echo -e "${GREEN}=====================================${NC}"
echo -e "${GREEN}SUCCESS! Installation Complete${NC}"
echo -e "${GREEN}=====================================${NC}"
echo ""
echo -e "${YELLOW}Next Steps:${NC}"
echo -e "${NC}  1. To run on Android device/emulator:${NC}"
echo -e "${CYAN}     npx cap run android${NC}"
echo ""
echo -e "${NC}  2. To open in Android Studio:${NC}"
echo -e "${CYAN}     npx cap open android${NC}"
echo ""
echo -e "${NC}  3. To run development server:${NC}"
echo -e "${CYAN}     npm run dev${NC}"
echo ""
echo -e "${NC}  4. To rebuild after changes:${NC}"
echo -e "${CYAN}     npm run build && npx cap sync android${NC}"
echo ""
