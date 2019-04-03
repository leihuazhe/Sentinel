var app = angular.module('sentinelDashboardApp');

app.service('IdentityService', ['$http', function ($http) {

  this.fetchIdentityOfMachine = function (ip, port, searchKey,app) {
    var param = {
      ip: ip,
      port: port,
      searchKey: searchKey,
      app: app
    };
    return $http({
      url: 'resource/machineResource.json',
      params: param,
      method: 'GET'
    });
  };
  this.fetchClusterNodeOfMachine = function (ip, port, searchKey,app) {
    var param = {
      ip: ip,
      port: port,
      type: 'cluster',
      searchKey: searchKey,
      app: app
    };
    return $http({
      url: 'resource/machineResource.json',
      params: param,
      method: 'GET'
    });
  };
}]);
