FROM eclipse-temurin:8u372-b07-jdk

RUN mkdir -p /home/ci

# Create an app user so our program doesn't run as root.
RUN groupadd -r ci &&\
    useradd -r -g ci -d /home/ci -s /sbin/nologin -c "Docker image user" ci

# Set the home directory to our app user's home.
ENV HOME=/home/ci
#sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 3FA7E0328081BFF6A14DA29AA6A19B38D3D831EF
#sudo apt-add-repository 'deb https://download.mono-project.com/repo/ubuntu stable-focal main'

RUN apt-get update \
    && apt-get install apt-transport-https dirmngr gnupg ca-certificates software-properties-common -y
RUN apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 3FA7E0328081BFF6A14DA29AA6A19B38D3D831EF \
    && add-apt-repository 'deb https://download.mono-project.com/repo/ubuntu stable-focal main'
    #&& echo "deb https://download.mono-project.com/repo/debian stable-stretch main" | tee /etc/apt/sources.list.d/mono-official-stable.list
RUN apt-get update \
    && apt-get install mono-devel -y \
    && apt-get clean

RUN chown -R ci:ci $HOME
RUN chmod -R 777 $HOME