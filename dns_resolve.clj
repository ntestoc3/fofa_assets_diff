#!/usr/bin/env bb

(defn run-dnsx [file]
  (println :run-dnsx (str file))
  (some->> (shell/sh "dnsx" "-a" "-silent" "-resp" "-l" (str file))
           :out
           str/split-lines
           (map #(let [[domain ip] (str/split % #"\s+")]
                   (try
                     [domain (str/replace ip #"\[|\]" "")]
                     (catch Exception e
                       (println "process" domain ":" ip "error" (ex-message e))
                       nil))))
           (filter identity)
           seq ;; fix lazy-seq apply
           (apply map vector)))


(require '[babashka.cli :as cli])

(def cli-options {:path {:default "./paypal/chaos"
                         :validate fs/directory?
                         :alias :p
                         :coerce fs/file
                         }
                  :out {:default "./paypal/chaos-dns"
                        :alias :o
                        :coerce fs/file
                        }
                  :help {:coerce :boolean
                         :alias :h}})

(defn print-opts
  []
  (println)
  (println "  options:")
  (println (cli/format-opts {:spec cli-options})))

(defn -main [& args]
  (let [{:keys [path out help]} (cli/parse-opts args {:spec cli-options})]
    (when help
      (print-opts)
      (System/exit 0))

    (when-not (fs/exists? out)
      (fs/create-dirs out))

    (doseq [f1 (-> (fs/expand-home path)
                   (fs/glob "*.txt"))]
      (println "dns process" (str f1))
      (let [[hosts _] (run-dnsx f1)]
        (when (seq hosts)
          (spit
           (str (fs/file out (fs/file-name f1)))
           (str/join "\n" (set hosts))))))))

(try
  (apply -main *command-line-args*)
  (catch Exception e
    (println "domain dns resolver:")
    (println "error: " (ex-message e) e)
    (print-opts)
    (System/exit 1)))

