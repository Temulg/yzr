(setq lsp-print-io t)

(setq yzr-root
      (file-name-directory (or load-file-name buffer-file-name)))

;; java

;;(setq lsp-java-trace-server 'verbose)
(setq lsp-java-java-path "/opt/oracle-jdk-bin-10.0.1/bin/java")
(setq lsp-java-vmargs (list
      "-noverify" "-Xmx1G" "-XX:+UseG1GC" "-XX:+UseStringDeduplication"
      "--add-modules=ALL-SYSTEM"
      "--add-opens" "java.base/java.util=ALL-UNNAMED"
      "--add-opens" "java.base/java.lang=ALL-UNNAMED"))

(setq lsp-java--workspace-folders
      (list yzr-root))

(setq lsp-java-import-gradle-enabled nil)
(setq lsp-java-import-maven-enabled nil)
(setq lsp-java-auto-build nil)

(require 'lsp-java)
(add-hook 'java-mode-hook #'lsp-java-enable)
