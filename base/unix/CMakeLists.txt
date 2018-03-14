project( unix )

set( JAVA_BOOTSTRAP_SRCS UnixBootstrap.java )
set(
	JAVA_BOOTSTRAP_CLASSES
	${CMAKE_CURRENT_BINARY_DIR}/yzr/base/UnixBootstrap.class
)

add_custom_command(
	OUTPUT ${JAVA_BOOTSTRAP_CLASSES}
	COMMAND ${JDK_PATH}/bin/javac
	ARGS -d ${CMAKE_CURRENT_BINARY_DIR} ${JAVA_BOOTSTRAP_SRCS}
	WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
	DEPENDS ${JAVA_BOOTSTRAP_SRCS}
)

add_custom_target( java_bootstrap DEPENDS ${JAVA_BOOTSTRAP_CLASSES} )

add_subdirectory( native )
