set( CMAKE_INSTALL_RPATH_USE_LINK_PATH TRUE )

set( GENERATED_DATA_FILES )

foreach( SRC ${JAVA_BOOTSTRAP_CLASSES} )
	get_filename_component( BASE_NAME ${SRC} NAME_WE )
	set( INC_NAME ${BASE_NAME}_data.h )
	add_custom_command(
		OUTPUT ${INC_NAME}
		COMMAND bin2cstr
		ARGS < ${SRC} > ${INC_NAME}
		DEPENDS java_bootstrap
	)
	list( APPEND GENERATED_DATA_FILES ${INC_NAME} )
endforeach( SRC )

add_executable(
	yzr
	main.c
	bootstrap.c
	${GENERATED_DATA_FILES}
)

target_include_directories(
	yzr
	PRIVATE ${CMAKE_CURRENT_BINARY_DIR}
	PRIVATE ${JDK_PATH}/include
	PRIVATE ${JDK_PATH}/include/linux
)

target_link_libraries(
	yzr
	${JDK_PATH}/lib/server/libjvm.so
)

install(
	TARGETS yzr RUNTIME
	DESTINATION ${CMAKE_BINARY_DIR}/image/bin
)
