FROM public.ecr.aws/amazoncorretto/amazoncorretto:21

WORKDIR /app
ADD target/smartfloor-*.jar /app/run.jar

EXPOSE 80:80
CMD java -jar run.jar