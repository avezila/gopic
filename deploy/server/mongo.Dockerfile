FROM       mongo:latest

COPY ./server/mongo /setup

WORKDIR /setup
ENTRYPOINT ["/setup/entrypoint.sh"]

EXPOSE 27017
CMD ["mongod"]
