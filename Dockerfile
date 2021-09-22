FROM ubuntu:14.04
MAINTAINER kw.cho <kw.cho@catenoid.net>

# Install the basic packages
RUN apt-get update && apt-get install -y software-properties-common git wget pkg-config

# 한글 설정
RUN apt-get install -y language-pack-ko
# set locale ko_KR
RUN locale-gen ko_KR.UTF-8
ENV LANG ko_KR.UTF-8
ENV LANGUAGE ko_KR.UTF-8
ENV LC_ALL ko_KR.UTF-8

# Install jdk1.7
#RUN add-apt-repository ppa:webupd8team/java && apt-get update
RUN apt-get update --fix-missing && apt-get install -y openjdk-7-jre jsvc

RUN ldconfig

# Install mediainfo
RUN apt-get update --fix-missing && apt-get install -y mediainfo libzen-dev libmediainfo-dev imagemagick

# Install x265 codec with yasm, cmake, g++ ...
RUN apt-get install -y yasm cmake g++ cmake-curses-gui
RUN wget https://bitbucket.org/multicoreware/x265/downloads/x265_1.9.tar.gz
RUN tar -zxvf x265_1.9.tar.gz x265_1.9
# compile x265 library
WORKDIR /x265_1.9/build
RUN cmake ../source
RUN make
RUN make install
RUN ldconfig

# Install ffmpeg with codecs...
WORKDIR /

# add repository fdk-aac library
RUN echo "\ndeb http://www.deb-multimedia.org/ wheezy main non-free" >> /etc/apt/sources.list
RUN apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 5C808C2B65558117
RUN apt-get update

# Install codecs...
RUN apt-get install -y libx264-dev libfdk-aac-dev libmp3lame-dev libtheora-dev libvorbis-dev libvpx-dev libopencore-amrwb-dev libopencore-amrnb-dev libwebp-dev libxvidcore-dev libxvidcore4
RUN ldconfig

# git clone ffmpeg lastest
RUN git clone https://github.com/catenoid-company/FFmpeg.git /ffmpeg
WORKDIR /ffmpeg
# git ffmpeg version fixed (ffmpeg version N-80425-g6031e5d)
#RUN git checkout 2ea5ab
# compile ffmpeg
RUN ./configure --pkg-config-flags="--static" --enable-gpl --enable-libmp3lame --enable-libtheora --enable-libvorbis --enable-libvpx --enable-libx264 --enable-libx265 --enable-libxvid --enable-libwebp --enable-libfdk_aac --enable-libopencore-amrnb --enable-libopencore-amrwb --enable-version3 --enable-nonfree
# SKYLIFE 송출서버를 위한 ffmpeg 소스 수정(mpegts_id값을 0x00으로 fixed)
RUN perl -p -i -e '$.==981 and print "\n    // ffmpeg 소스 수정. SKYLIFE 송출서버에서 tsid값이 0x00만 송출이 가능하다고 함.\n    // 따라서, tsid값을 0x00으로 fixed처리함.\n    ts->tsid = 0x00;\n"' /ffmpeg/libavformat/mpegtsenc.c
RUN make
RUN make install
RUN ldconfig
RUN make distclean

WORKDIR /

RUN rm -rf /x265_1.9 && rm -rf /ffmpeg && rm -rf /var/lib/apt/lists/*

# kollus 계정 & 그룹 생성
RUN groupadd -g 2001 kollus
RUN useradd -u 2001 -g kollus -m -d /home/kollus kollus

# kollus 홈 디렉터리 생성
RUN mkdir -p /home/kollus

RUN mkdir -p /home/kollus/MediaWatcher2 && \
	mkdir -p /home/kollus/MediaWatcher2/conf && \
	mkdir -p /home/kollus/MediaWatcher2/logs && \
	mkdir -p /home/kollus/MediaWatcher2/snapshot && \
	mkdir -p /home/kollus/MediaWatcher2/snap.temp && \
	mkdir -p /home/kollus/upload && \
	mkdir -p /home/kollus/http_upload && \
	mkdir -p /home/kollus/http_endpoint_upload

# 디렉터리 소유자&그룹 설정
RUN chown -R kollus.kollus /home/kollus

# copy MediaWatcher2 binary
ADD ./MediaWatcher2.jar /home/kollus/MediaWatcher2/
ADD ./MediaWatcher2 /home/kollus/MediaWatcher2/
ADD ./ls.sh /home/kollus/MediaWatcher2/
#ADD ./jar_rename_restart.sh /home/kollus/MediaWatcher2/
#ADD ./conf/watcher.json /home/kollus/MediaWatcher2/conf
#ADD ./conf/log4j.properties /home/kollus/MediaWatcher2/conf

RUN sudo chown -R kollus.kollus /home/kollus/MediaWatcher2/*

# 동작 계정 설정
USER kollus

# 디렉터리 host 공유 설정
VOLUME ["/home/kollus/MediaWatcher2", "/home/kollus/upload", "/home/kollus/http_upload"]

WORKDIR /home/kollus/MediaWatcher2

#CMD ["/bin/bash", "-c", "/home/kollus/MediaWatcher2/MediaWatcher2"]
CMD ["/usr/bin/java", "-jar", "/home/kollus/MediaWatcher2/MediaWatcher2.jar", "start"]

EXPOSE 8088
#EXPOSE 443
