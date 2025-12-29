# Beginner Guide: Docker and Apache JMeter
This guide introduces Docker and Apache JMeter for users who are new to these tools. It is designed to help you set up the environment required to run E2E-Loader.

## 1. Introduction to Docker 🐳
Docker is an open-source platform designed to simplify the process of building, shipping, and running applications. It achieves this by packaging applications and all their dependencies into containers, which are lightweight, portable, and consistent across different environments.
Key concepts in Docker are those of **Images** and **Containers**. We briefly introduce them in the following, but readers interested in learning more are recommended to visit the official Docker documentation here: https://docs.docker.com/get-started/docker-overview/

### What is a Container?
A container is an isolated environment that includes everything needed to run an application: the code, runtime, libraries, and configuration files. 
Containers share the host operating system's kernel but remain independent from other containers, ensuring that applications do not interfere with each other.

### What is an Image?
An image is the blueprint for a container. It defines the application and its dependencies in a read-only format. 
When you create or run a container, Docker uses the image as the starting point. Images can be versioned, stored in repositories, and shared easily across teams.

### Why Docker?
E2E-Loader requires Docker to run  one of its internal modules, namely the **Schema Generator**. 
This component depends on a specific version of the Node.js runtime. Without Docker, users would need to manually install and configure Node.js and its dependencies, which can be error-prone and time-consuming. 
Docker eliminates this complexity by packaging the required environment into a container, ensuring that everything works out of the box.
Advantages of Docker for E2E-Loader include:

- **Consistency**: Docker guarantees that the Schema Generator runs the same way on any machine, regardless of the underlying operating system or installed software. This avoids the classic "works on my machine" problem.
- **Isolation**: Each Docker container is independent from the host system and other containers. This means the Node.js environment required by the Schema Generator will not interfere with other applications or libraries on your machine.
- **Ease of Deployment**: Docker simplifies installation and configuration. Instead of installing Node.js manually, you only need to pull and run the Docker image. All dependencies are pre-configured, reducing setup time and minimizing errors.
- **Portability**: Docker images can be shared easily across teams and environments. Whether you are running E2E-Loader on Windows, macOS, or Linux, the Docker container ensures identical behavior everywhere.
- **Reproducibility**: Using Docker ensures that the same versions of Node.js and other dependencies are used consistently, which is critical for reproducible performance testing results.

### Installing Docker
The easiest way to install Docker is to install the GUI Tool called Docker Desktop. Details on how to do that and on different installation options are available on the official website here: https://docs.docker.com/get-started/get-docker/


## 2. Apache JMeter ⚡

Apache JMeter is a widely used open-source tool designed for performance and load testing of web applications, APIs, and other services. It helps developers and testers evaluate how systems 
behave under different levels of stress by simulating real-world user activity.

### What Does JMeter Do?
JMeter works by sending requests to your application and measuring its responses. It can simulate thousands of concurrent users performing various actions, such as browsing pages, submitting forms, or interacting with APIs.
This allows teams to identify performance bottlenecks, scalability issues, and potential failures before deploying applications to production.

### Key Features of JMeter
- **Protocol Support**: JMeter supports multiple protocols, including HTTP, HTTPS, WebSocket, FTP, JDBC, and more, making it versatile for different types of applications.
- **Load Testing**: It enables testers to create scenarios that mimic real user behavior under normal and peak load conditions.
- **Stress and Spike Testing**: JMeter can simulate extreme conditions, such as sudden traffic spikes, to evaluate system resilience.
- **Extensibility**: Through plugins and scripting, JMeter can be customized to fit complex testing needs.
- **Reporting and Analysis**: JMeter provides detailed metrics on response times, throughput, error rates, and resource utilization, helping teams analyze performance trends.

### Why JMeter?
E2E-Loader uses JMeter as a reference performance testing tool, generating workloads that can be executed or further edited using JMeter. We chose JMeter because it is:
- **Open Source and Free**: JMeter is cost-effective and widely adopted in both industry and academia.
- **Cross-Platform**: It runs on any system with a Java Virtual Machine (JVM), including Windows, macOS, and Linux.

### Installing JMeter
1. **Check Java Installation**. JMeter requires a Java Virtual Machine (JVM). Make sure Java is installed.
You can check with: `java -version`. If not installed, download Java from https://www.oracle.com/java/technologies/downloads/ or use https://openjdk.org/install/.

2. **Download JMeter**.
Go to the official download page: https://jmeter.apache.org/download_jmeter.cgi
Even though E2E-Loader should work without issues with any recent release of JMeter, it was thoroughly tested with version **5.5**, so we recommend you download that version. Download the binary archive (ZIP or TGZ). Download links are available here: https://archive.apache.org/dist/jmeter/binaries/.

3. **Extract the Archive**. Unzip or untar the downloaded file into a directory of your choice.

4. **Run JMeter**. Navigate to the bin folder inside the extracted directory. You can then launch JMeter using `./jmeter.sh` on Linux/MacOS, or `jmeter.bat` on Windows. Alternatively, you should be able to start JMeter also by double-clicking on the `jmeter.jar` file.

5. **Verify Installation**. The JMeter GUI should open. You can now start creating or editing performance/stress testing workloads.

### Learning More About JMeter
Users interested in learning more about JMeter are invited to checkout the official getting started page at: https://jmeter.apache.org/usermanual/get-started.html
