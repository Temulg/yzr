cmake_minimum_required( VERSION 3.3 )
project( yzr )

set( JDK_PATH /opt/oracle-jdk-bin-9.0.1 CACHE PATH "path to JDK" )

add_subdirectory( base/buildtools )

if( UNIX )
	add_subdirectory( base/unix )
elseif( WIN32 )
	message( FATAL_ERROR "Not yet supported" )
endif()
