package org.sunbird.obsrv.connector

object EventFixture {

  val INVALID_JSON = """{"name":"/v1/sys/health","context":{"trace_id":"7bba9f33312b3dbb8b2c2c62bb7abe2d""""
  val VALID_JSON = """{"name":"/v1/sys/health","context":{"trace_id":"7bba9f33312b3dbb8b2c2c62bb7abe2d","span_id":"086e83747d0e381e"},"parent_id":"","start_time":"2021-10-22 16:04:01.209458162 +0000 UTC","end_time":"2021-10-22 16:04:01.209514132 +0000 UTC","status_code":"STATUS_CODE_OK","status_message":"","attributes":{"net.transport":"IP.TCP","net.peer.ip":"172.17.0.1","net.peer.port":"51820","net.host.ip":"10.177.2.152","net.host.port":"26040","http.method":"GET","http.target":"/v1/sys/health","http.server_name":"mortar-gateway","http.route":"/v1/sys/health","http.user_agent":"Consul Health Check","http.scheme":"http","http.host":"10.177.2.152:26040","http.flavor":"1.1"},"events":[{"name":"","message":"OK","timestamp":"2021-10-22 16:04:01.209512872 +0000 UTC"}]}"""

  val EVENT_1 = """{"tripID":"de7657aa-2ae7-46c9-b8fa-c909381a1419","VendorID":"1","tpep_pickup_datetime":"2024-01-31 00:46:40","tpep_dropoff_datetime":"2024-01-31 00:53:20","passenger_count":"1","trip_distance":"1.50","RatecodeID":"1","store_and_fwd_flag":"N","PULocationID":"151","DOLocationID":"239","payment_type":"1","primary_passenger":{"email":"Gladys.Dietrich@hotmail.com","mobile":"(777) 293-4409 x0027"},"fare_details":{"fare_amount":7,"extra":0.5,"mta_tax":0.5,"tip_amount":1.65,"tolls_amount":0,"improvement_surcharge":0.3,"total_amount":9.95,"congestion_surcharge":0}}"""
  val EVENT_2 = """{"tripID":"f834d607-6b29-46e1-b1a5-7f3b6028fefb","VendorID":"1","tpep_pickup_datetime":"2023-10-08 00:59:47","tpep_dropoff_datetime":"2023-10-08 01:18:59","passenger_count":"1","trip_distance":"2.60","RatecodeID":"1","store_and_fwd_flag":"N","PULocationID":"239","DOLocationID":"246","payment_type":"1","primary_passenger":{"email":"Louvenia_Harris@yahoo.com","mobile":"927.905.6035"},"fare_details":{"fare_amount":14,"extra":0.5,"mta_tax":0.5,"tip_amount":1,"tolls_amount":0,"improvement_surcharge":0.3,"total_amount":16.3,"congestion_surcharge":0}}"""
  val EVENT_3 = """{"tripID":"734bd177-3ce2-4731-a650-f8365ff55291","VendorID":"2","tpep_pickup_datetime":"2023-06-27 13:48:30","tpep_dropoff_datetime":"2023-06-27 13:52:40","passenger_count":"3","trip_distance":".00","RatecodeID":"1","store_and_fwd_flag":"N","PULocationID":"236","DOLocationID":"236","payment_type":"1","primary_passenger":{"email":"Alexis_Kihn4@hotmail.com","mobile":"1-701-364-0121 x3238"},"fare_details":{"fare_amount":4.5,"extra":0.5,"mta_tax":0.5,"tip_amount":0,"tolls_amount":0,"improvement_surcharge":0.3,"total_amount":5.8,"congestion_surcharge":0}}"""
}