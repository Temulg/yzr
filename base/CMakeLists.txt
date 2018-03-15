add_subdirectory( buildtools )

set( BASE_MODULE_PATH ${CMAKE_CURRENT_BINARY_DIR}/target )
set( BASE_MODULE ${BASE_MODULE_PATH}/base-1.0.jar )
file( GLOB_RECURSE BASE_MODULE_SRCS src )

add_custom_command(
	OUTPUT ${BASE_MODULE}
	COMMAND mvn
	ARGS -Dtarget.dir=${BASE_MODULE_PATH} package
	WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
	DEPENDS ${BASE_MODULE_SRCS}
)

add_custom_target( app_module DEPENDS ${BASE_MODULE} )

if( UNIX )
	add_subdirectory( unix )
elseif( WIN32 )
	message( FATAL_ERROR "Not yet supported" )
endif()
