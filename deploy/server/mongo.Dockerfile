FROM       mongo:latest

COPY ./server/mongo /setup

ENTRYPOINT ["/setup/entrypoint.sh"]

EXPOSE 27017
CMD ["mongod"]
