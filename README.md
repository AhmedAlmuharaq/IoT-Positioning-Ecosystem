# IoT Positioning & Mobility Ecosystem 🛰️

This repository contains a comprehensive hybrid positioning and mobility prediction ecosystem developed as part of the Positioning Systems, Techniques, and Applications (PSTA) module (Master IoT). It demonstrates the transition from reactive geometric localization to proactive, statistical mobility prediction.

## 🌟 Project Modules

The project is divided into three core algorithms, unified by a custom Interactive Web Dashboard.

### 1. 3D N-Lateration (Geometric Localization)
An algorithm designed for outdoor, line-of-sight environments. It calculates the exact 3D coordinates $(X, Y, Z)$ of a mobile terminal by evaluating its distance from multiple anchors.
* **Methodology:** Uses a Brute-Force spatial walk algorithm within a dynamically calculated bounding box to minimize the L1 Norm Error.
* **Simulation:** Calculates coordinate intersections dynamically and visualizes them using 3D wireframe spheres.

### 2. Static Fingerprinting (Indoor Localization)
Designed to bypass indoor signal multipath fading by relying on an offline radiomap database rather than geometric equations.
* **Methodology:** Applies the **K-Nearest Neighbors (K-NN)** algorithm using Manhattan distance to match real-time RSSI vectors.
* **Optimization:** Uses **Inverse Distance Weighting (IDW)** to calculate a highly precise, weighted barycenter for the final coordinate.

### 3. Mobility Prediction (Hidden Markov Models - HMM)
Introduces predictive intelligence to forecast a user's navigation state transitions. 
* **Methodology:** Implements a dynamic $6 \times 6$ transition matrix.
* **Metrics:** Calculates both **Forward Probabilities** (Next State) and **Backward Probabilities** (Previous State).
* **Simulation:** Visualizes the Markov memoryless property through an interactive, physics-based directed graph.

## 🚀 The Web Application (SPA)
To unify these algorithms, the mathematical models were ported into a highly interactive Single Page Application (SPA). 
* **Tech Stack:** HTML5, CSS3, Vanilla JavaScript, `Plotly.js` (for 3D/2D plotting), and `Vis.js` (for HMM network graphs).
* **Features:** Allows real-time manipulation of variables (e.g., alternating $N=3$ or $N=4$ anchors, adjusting $K$-neighbors, and interactive terminal simulation).
* **Usage:** Simply open `index.html` in any modern web browser to launch the dashboard. No server required.

## 👨‍💻 Development Team
* **Ahmed Almuharaq**
* Amine
* Oumnia