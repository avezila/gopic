FROM       scratch

ADD        server/build/gopic /gopic
ENV        PORT 5353
EXPOSE     5353
ENTRYPOINT ["/gopic"]
